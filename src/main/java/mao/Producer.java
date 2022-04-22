package mao;

import com.rabbitmq.client.Channel;
import mao.tools.RabbitMQ;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Project name(项目名称)：rabbitMQ消息发布确认之批量确认发布
 * Package(包名): mao
 * Class(类名): Producer
 * Author(作者）: mao
 * Author QQ：1296193245
 * GitHub：https://github.com/maomao124/
 * Date(创建日期)： 2022/4/22
 * Time(创建时间)： 18:45
 * Version(版本): 1.0
 * Description(描述)：
 * 与单个等待确认消息相比，先发布一批消息然后一起确认可以极大地
 * 提高吞吐量，当然这种方式的缺点就是:当发生故障导致发布出现问题时，不知道是哪个消息出现
 * 问题了，我们必须将整个批处理保存在内存中，以记录重要的信息而后重新发布消息。当然这种
 * 方案仍然是同步的，也一样阻塞消息的发布。
 *
 * 1000条消息：59.159毫秒
 * 速度：39000条每秒
 */

public class Producer
{
    private static final String QUEUE_NAME = "work";

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException
    {
        Channel channel = RabbitMQ.getChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //在此信道上启用发布者确认
        channel.confirmSelect();
        //批量确认消息大小
        int batchSize = 50;
        //未确认消息个数
        int outstandingMessageCount = 0;

        //------------------------------------------------------
        long startTime = System.nanoTime();   //获取开始时间
        //------------------------------------------------------
        for (int i = 0; i < 1000;i++)
        {
            String message = "消息" + (i + 1);
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            //等到自上次调用以来发布的所有消息都已被代理确认或确认。
            // 请注意，当在非确认通道上调用时，waitForConfirms 会引发 IllegalStateException。
            outstandingMessageCount++;
            if (outstandingMessageCount == batchSize)
            {
                boolean waitForConfirms = channel.waitForConfirms();
                if (waitForConfirms)
                {
                    System.out.println("一批消息发送成功");
                }
                else
                {
                    System.out.println("消息发送失败");
                }
                outstandingMessageCount = 0;
            }
        }
        //------------------------------------------------------
        long endTime = System.nanoTime(); //获取结束时间
        if ((endTime - startTime) < 1000000)
        {
            double final_runtime;
            final_runtime = (endTime - startTime);
            final_runtime = final_runtime / 1000;
            System.out.println("算法运行时间： " + final_runtime + "微秒");
        }
        else if ((endTime - startTime) >= 1000000 && (endTime - startTime) < 10000000000L)
        {
            double final_runtime;
            final_runtime = (endTime - startTime) / 1000;
            final_runtime = final_runtime / 1000;
            System.out.println("算法运行时间： " + final_runtime + "毫秒");
        }
        else
        {
            double final_runtime;
            final_runtime = (endTime - startTime) / 10000;
            final_runtime = final_runtime / 100000;
            System.out.println("算法运行时间： " + final_runtime + "秒");
        }
        Runtime r = Runtime.getRuntime();
        float memory;
        memory = r.totalMemory();
        memory = memory / 1024 / 1024;
        System.out.printf("JVM总内存：%.3fMB\n", memory);
        memory = r.freeMemory();
        memory = memory / 1024 / 1024;
        System.out.printf(" 空闲内存：%.3fMB\n", memory);
        memory = r.totalMemory() - r.freeMemory();
        memory = memory / 1024 / 1024;
        System.out.printf("已使用的内存：%.4fMB\n", memory);
        //------------------------------------------------------

        System.out.println("消息全部发送完成");
    }
}
