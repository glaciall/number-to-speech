package cn.org.hentai.nts;

import java.io.FileOutputStream;

public class Test
{
    public static void main(String[] args) throws Exception
    {
        // 此处返回的pcm字节数组的内容，可以通过ffplay -f s16le -ar 16000 -ac 1来播放
        byte[] pcm = NumberToSpeech.toVoice("12345.04", "alipay", "yuan");

        // 这里我们比较随意的加个wav头，来保存到wav文件里，好进行测试播放
        FileOutputStream fos = new FileOutputStream("d:\\test.wav");

        // 资源交换文件标志
        fos.write(new byte[] { 0x52, 0x49, 0x46, 0x46 });
        // 后续数据长度
        fos.write(toBytes(pcm.length + 36));
        // WAVE标记
        fos.write("WAVE".getBytes());
        // 波形格式标志
        fos.write("fmt ".getBytes());
        // FMT块大小
        fos.write(toBytes(0x00000010));
        // 音频编码：线性PCM
        fos.write(0x01);
        fos.write(0x00);
        // 声道数：1
        fos.write(0x01);
        fos.write(0x00);
        // 每秒样本数：16000
        fos.write(toBytes(0x3e80));
        // 每秒字节数：32000
        fos.write(toBytes(0x7d00));
        // 每样本字节数：2
        fos.write(0x02);
        fos.write(0x00);
        // 每样本比特数：16
        fos.write(0x10);
        fos.write(0x00);
        // 数据体标志
        fos.write("data".getBytes());
        // 数据体长度
        fos.write(toBytes(pcm.length));
        // 写入音频数据体
        fos.write(pcm);
        fos.flush();
        fos.close();
    }

    private static byte[] toBytes(int v)
    {
        byte[] x = new byte[4];
        x[0] = (byte)(v & 0xff);
        x[1] = (byte)((v >> 8) & 0xff);
        x[2] = (byte)((v >> 16) & 0xff);
        x[3] = (byte)((v >> 24) & 0xff);

        return x;
    }
}