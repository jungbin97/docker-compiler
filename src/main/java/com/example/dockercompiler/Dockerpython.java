package com.example.dockercompiler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.Duration;

public class Dockerpython {
    public static void main(String[] args) throws Exception{
        // python 코드
        String pythonCode = "print('hello wolrd!')";

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
        dockerClient.pullImageCmd("python:3.9.18").exec(new PullImageResultCallback()).awaitCompletion();

        CreateContainerResponse container = dockerClient.createContainerCmd("python:3.9.18")
                .withCmd("python", "-c", pythonCode)
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
