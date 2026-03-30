package com.bagdatahouse.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 数据中台工具 - 启动类
 * 
 * @author bagdatahouse
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.bagdatahouse"})
@MapperScan({"com.bagdatahouse.core.mapper", "com.bagdatahouse.server.mapper"})
public class BagdatahouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BagdatahouseApplication.class, args);
        System.out.println("""
            
            ███████╗██╗██╗  ██╗ ██████╗ ███████╗██████╗ ███████╗
            ██╔════╝██║╚██╗██╔╝██╔════╝ ██╔════╝██╔══██╗██╔════╝
            █████╗  ██║ ╚███╔╝ ██║  ███╗█████╗  ██████╔╝███████╗
            ██╔══╝  ██║ ██╔██╗ ██║   ██║██╔══╝  ██╔══██╗╚════██║
            ██║     ██║██╔╝ ██╗╚██████╔╝███████╗██║  ██║███████║
            ╚═╝     ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝
            
            数据中台工具 - 启动成功！
            Swagger文档地址: http://localhost:8080/api/doc.html
            Knife4j文档: http://localhost:8080/api/doc.html
            """);
    }
}
