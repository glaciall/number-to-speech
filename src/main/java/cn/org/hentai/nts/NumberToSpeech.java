/*
Copyright [2021] [matrixy]
www.hentai.org.cn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.org.hentai.nts;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * 数值语音合成，输入一个数字，输出一个PCM_LE 16bit、16000采样率、单声道的PCM音频数据
 * @Author: matrixy
 * @Email: wlhack@sina.com
 * @Created: 2021/03/24
 * @WebSite: www.hentai.org.cn
 */

public final class NumberToSpeech
{
    // 单位后缀，与素材wav文件相匹配
    static final String[] units = new String[] { "", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000", "000000000" };

    /**
     * 获取每个数字值的语音文件代号，可用于调试或其它目的
     * @param number 数字字符串
     * @return 数字字符串每个位上的声音文件代号列表
     */
    public static List<String> getBits(String number)
    {
        // 去掉末尾无意义的.00000
        number = number.replaceAll("^\\.0*$", "");

        int len = number.length();
        int dianIndex = -1;
        LinkedList<String> bits = new LinkedList();

        // 取整数上每一个数字的音频位名称
        int dotIndex = number.indexOf('.') - 1;
        if (dotIndex < 0) dotIndex = number.length() - 1;
        for (int i = 0, k = 0; i < len && i <= dotIndex; i++, k++)
        {
            char chr = number.charAt(i);
            if (chr == '.') break;
            String unit = units[dotIndex - k];
            bits.addLast("n" + chr + unit);
        }

        // 去掉连续的零
        boolean zero = false;
        for (int i = 0, l = bits.size(); i < l; i++)
        {
            String x = bits.get(i);
            if (x.matches("^n0*$"))
            {
                if (!zero) bits.set(i, "n0");
                else bits.set(i, null);
                zero = true;
            }
            else zero = false;

            // 最后一个是0，而且数字已经超过1个时
            if (i == l - 1 && x.equals("n0") && bits.size() > 1) bits.set(i, null);
        }

        // 小数位
        dotIndex = number.indexOf('.');
        if (dotIndex > -1)
        {
            for (int i = dotIndex; i < len; i++)
            {
                char chr = number.charAt(i);
                if (chr == '.')
                {
                    bits.addLast("dian");
                    dianIndex = bits.size();
                }
                else bits.addLast("n" + chr);
            }
        }

        // 删除null单元
        boolean dian = false;
        for (int i = bits.size() - 1; i >= 0; i--)
        {
            String bit = bits.get(i);
            if (bit == null)
            {
                bits.remove(i);
                continue;
            }
            // 删除掉dian/整数位起之前的多出来的n0
            if (bit.equals("dian")) dian = true;
            if (dian && bit.equals("n0") && i > 0)
            {
                bits.remove(i);
                continue;
            }
            // 如果没有小数点，则直接删除最后一个n0
            if (dianIndex == -1 && i == bits.size() - 1 && bit.equals("n0"))
            {
                bits.remove(i);
            }
        }

        return bits;
    }

    /**
     * 将number转换为PCM音频数据，并将prefix前缀音频和suffix后缀音频也组合起来
     * @param number 待转换的数字字符串
     * @param prefix 前缀音频文件名
     * @param suffix 后缀音频文件名
     * @return PCM LE、16bit、16000采样率的单声道音频数据
     */
    public static byte[] toVoice(String number, String prefix, String suffix)
    {
        List<String> segments = getBits(number);
        if (prefix != null) segments.add(0, prefix);
        if (suffix != null) segments.add(suffix);

        ByteArrayOutputStream pcm = new ByteArrayOutputStream(409600);

        for (String name : segments)
        {
            byte[] segment = readVoice(name);

            // 消除末尾的静音
            int stopIndex = 0;
            for (int i = segment.length - 1; i >= 0; i-=2)
            {
                int a = segment[i] & 0xff, b = segment[i - 1] & 0xff;
                int c = ((a << 8) | b) & 0xffff;
                if (c == 0x00) continue;
                else
                {
                    stopIndex = i;
                    break;
                }
            }

            for (int i = 0; i <= stopIndex; i++)
            {
                pcm.write(segment[i]);
            }

            // 如果需要，可以在每一个数字的末尾加一定量的静音停顿
            // 此处hms表示百毫秒，由于每秒需要32000个字节，那么每100毫秒就是3200个字节
            int hms = 2;
            for (int i = 0; i < hms; i++)
            {
                for (int k = 0; k < 3200; k++)
                {
                    pcm.write(0x00);
                }
            }
        }
        return pcm.toByteArray();
    }

    // 从wav文件中读取PCM数据
    static byte[] readVoice(String name)
    {
        ByteArrayOutputStream baos = null;
        InputStream input = null;
        try
        {
            baos = new ByteArrayOutputStream(40960);
            input = NumberToSpeech.class.getResourceAsStream("/audio/" + name + ".wav");
            input.skip(44);

            int len = -1;
            byte[] buff = new byte[512];
            while ((len = input.read(buff)) > -1)
            {
                baos.write(buff, 0, len);
            }
            return baos.toByteArray();
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            try { input.close(); } catch(Exception e) { }
        }
    }
}