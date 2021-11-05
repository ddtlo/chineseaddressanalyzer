package com.sixtofly.chineseaddressanalyzer.analyzer;

import cn.hutool.core.util.ArrayUtil;
import com.sixtofly.chineseaddressanalyzer.entity.dto.AddressDetailDto;
import com.sixtofly.chineseaddressanalyzer.service.AddressAnalyzeService;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author xie yuan bing
 * @date 2020-03-27 11:07
 * @description
 */
@Slf4j
@SpringBootTest
class AddressAnalyzerTest {

    @Autowired
    private AddressAnalyzeService analyzeService;

    /**
     * 分词测试
     */
    @Test
    public void splits() {
        System.out.println(AddressAnalyzer.analyze("四川成都双流高新区姐儿堰路远大都市风景二期"));
        System.out.println(AddressAnalyzer.analyze("成都市温江永文路南端1688号西贵堂"));
        System.out.println(AddressAnalyzer.analyze("四川省巴中市北云台水韵名城2-21-5"));
        System.out.println(AddressAnalyzer.analyze("青海省南宁市城北区金帝花园"));
    }

    /**
     * 地址解析测试
     */
    @Test
    public void analyze() {
        System.out.println(analyzeService.parseAddress("四川成都双流高新区姐儿堰路远大都市风景二期"));
        System.out.println(analyzeService.parseAddress("新疆克拉玛依市拉玛依区油建南路雅典娜74-76"));
        System.out.println(analyzeService.parseAddress("江西省萍乡市经济开发区西区工业园（硖石）金丰路23号江西省萍乡联友建材有限公司"));
        System.out.println(analyzeService.parseAddress("甘肃省嘉峪关前进路车务段9号"));
        System.out.println(analyzeService.parseAddress("四川省新津县五津镇武阳中路167号2栋1单元6楼2号"));
        System.out.println(analyzeService.parseAddress("双流县海棠湾2栋2单元1102号"));
        System.out.println(analyzeService.parseAddress("成都市双流区航空港康桥品上2栋1单元1704室"));
        System.out.println(analyzeService.parseAddress("重庆市江北区南桥寺光华南桥人家2-3-1-1"));
    }

    /**
     * 地址解析测试
     *
     * @throws IOException
     */
    @Test
    public void multiAnalyze() throws IOException {

        // 测试地址数据
        for (int i = 1; i < 4; i++) {
            parseAddress("src/test/resources/测试地址" + i + ".txt");
        }

        // 测试错误数据
        parseAddress("src/test/resources/error.txt");
    }

    private void parseAddress(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String address = null;
        while ((address = reader.readLine()) != null) {
            System.out.println(analyzeService.parseAddress(address));
        }
    }

    @Test
    public void syncTest() throws IOException {
        try {
//            File file = new File("/布管家数据.txt");
//            if (file.isFile() && file.exists()) {
//                InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
            InputStream isr = this.getClass().getResourceAsStream("/布管家数据.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(isr));
            String lineTxt = null;
            List<String> txtList = new ArrayList<>();
            List<Runnable> runnables = new ArrayList<>();
            List<AddressDetailDto> ads = new ArrayList<>();
            while ((lineTxt = br.readLine()) != null) {
//                    System.out.println(lineTxt);
                txtList.add(lineTxt);
                String finalLineTxt = lineTxt;
                runnables.add(new Runnable() {
                    @Override
                    public void run() {
                        AddressDetailDto addressDetailDto = analyzeService.parseAddress(finalLineTxt);
                        if (addressDetailDto != null) {
                            ads.add(analyzeService.parseAddress(finalLineTxt));
                        }
                    }
                });
            }
            br.close();
            parallelTesk(runnables.size(), runnables);
            System.out.println("转换完成");
        } catch (Exception e) {
            System.out.println("文件读取错误!");
        }
    }

    /**
     * 高并发测试：
     * 创建threadNum个线程；
     * 执行任务task
     *
     * @param threadNum 线程数量
     * @param tasks     任务 Runnable[] tasks
     */
    private static void parallelTesk(int threadNum, List<Runnable> tasks) {

        // 1. 定义闭锁来拦截线程
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(threadNum);

        // 2. 创建指定数量的线程
        for (int i = 0; i < threadNum; i++) {
            int index = i;
            Thread t = new Thread(() -> {
                try {
                    startGate.await();
                    try {
                        tasks.get(index).run();
                    } finally {
                        endGate.countDown();
                    }
                } catch (InterruptedException e) {

                }
            });

            t.start();
        }

        // 3. 线程统一放行，并记录时间！
        long start = System.nanoTime();

        startGate.countDown();
        try {
            endGate.await();
        } catch (InterruptedException e) {
        }

        long end = System.nanoTime();
        System.out.println("cost times :" + (end - start));
    }
}