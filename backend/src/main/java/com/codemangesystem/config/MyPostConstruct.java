package com.codemangesystem.config;

import com.codemangesystem.gitProcess.repository.ProjectRepository;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import com.codemangesystem.loginProcess.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory;

/**
 * 負責處理當 SpringBoot 建構起來後的初始設定 (所有 @Bean 都已相注入 Spring 容器內)
 */
@Component
@Slf4j
public class MyPostConstruct {
    public static String path = "src/cloneCode";

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final MyUserRepository myUserRepository;

    public MyPostConstruct(ProjectRepository projectRepository, UserService userService, MyUserRepository myUserRepository) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.myUserRepository = myUserRepository;
    }

    /**
     * 系統初始化設定
     * 包含：刪除 clone 檔案、清空 Project 相關資料、建立超級帳號
     */
    @PostConstruct
    public void initialSettings() {
        log.info("系統的初始化設定");

        try {
            // 刪除 src/cloneCode 內 clone 的資料
            deleteCloneFile();

            // 刪除 project -> files -> method -> diff_info 的相對應資料
            deleteProjectDatBase();

            // 如果需要新增超級帳號
            addSuperAccount();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 刪除 clone 下來的所有資料夾
     */
    public void deleteCloneFile() throws IOException {
        File file = new File(path);
        log.info("執行刪除 {} 資料夾", path);
        for (File subfile : Objects.requireNonNull(file.listFiles())) {
            String fileName = subfile.getName();
            if (subfile.isDirectory()) {
                deleteDirectory(subfile);
                log.info("刪除資料夾 {}", fileName);
            }
        }
    }

    /**
     * 刪除資料庫內所有的資料
     */
    public void deleteProjectDatBase() {
        log.info("刪除 project 內的資料");
        projectRepository.deleteAll();
    }

    /**
     * 檢查超級帳號是否存在，不存在則建立
     */
    public void addSuperAccount() {
        if (myUserRepository.findByUserAccount("123")
                            .isEmpty()) {
            log.info("加入超級帳號");
            userService.addSuperAccount();
        } else {
            log.info("超級帳號已存在");
        }
    }
}
