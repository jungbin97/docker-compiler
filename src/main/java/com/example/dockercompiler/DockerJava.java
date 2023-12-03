package com.example.dockercompiler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.Duration;

public class DockerJava {
    public static void main(String[] args) throws Exception{
        // java 코드
        String javaCode = "public class HelloWorld {" +
                "public static void main(String[] args) {" +
                "System.out.println(\"Hello, world!!!!!\");"+
                "}"+
                "}";


        // docker 클라이언트 생성
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();


        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(standard.getDockerHost())       // 호스트 설정
                .sslConfig(standard.getSSLConfig())
                .maxConnections(100)                  // 최대 동시 연결 수 설정
                .connectionTimeout(Duration.ofSeconds(30))  // 연결 시도 타임아웃 최대 30초 설정
                .responseTimeout(Duration.ofSeconds(45))    // 응답 대기 시간 45초 설정
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(standard, httpClient);


        // 도커 이미지로 컨테이너 생성
        try {
            dockerClient.pullImageCmd("openjdk:11").exec(new PullImageResultCallback()).awaitCompletion();
        } catch (NotFoundException e) {
            System.out.println("이미지를 찾을 수 없습니다: " + e.getMessage());
            return;
        }

        // Java 코드 파일로 작성하고 컴파일 및 실행하는 커맨드
        String[] commands = {
                "/bin/sh", "-c",
                "echo '" + javaCode + "' > HelloWorld.java && " +
                        "javac HelloWorld.java && " +
                        "java HelloWorld"
        };

        CreateContainerResponse container = dockerClient.createContainerCmd("openjdk:11")
                .withCmd(commands)
                .exec();

        // 컨테이너 시작
        dockerClient.startContainerCmd(container.getId()).exec();

        // 로그 처리
        String logs = dockerClient.logContainerCmd(container.getId())
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame item) {
                        System.out.println(new String(item.getPayload()));
                    }
                })
                .toString();

        System.out.println("Logs : " + logs);


        // 컨테이너 정지 및 제거
        dockerClient.stopContainerCmd(container.getId()).exec();
        dockerClient.removeContainerCmd(container.getId()).exec();
    }
}
