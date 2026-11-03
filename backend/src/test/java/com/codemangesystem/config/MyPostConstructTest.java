package com.codemangesystem.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.codemangesystem.git_process.repository.ProjectRepository;
import com.codemangesystem.login_process.service.UserService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

@Slf4j
class MyPostConstructTest {

  private static final String DEFAULT_PATH = MyPostConstruct.path;

  private ProjectRepository projectRepository;
  private UserService userService;
  private MyPostConstruct myPostConstruct;

  @BeforeEach
  void initial() {
    userService = mock(UserService.class);
    projectRepository = mock(ProjectRepository.class);
    myPostConstruct = new MyPostConstruct(projectRepository, userService);
  }

  /**
   * path 是 static，若某個測試改了它卻在中途失敗，會污染之後的測試，因此一律在這裡還原
   */
  @AfterEach
  void restorePath() {
    MyPostConstruct.path = DEFAULT_PATH;
  }

  @Test
  @DisplayName("測試是否正常刪除資料夾（只在暫存目錄操作，不碰真正的 src/cloneCode）")
  void deleteCloneFileTest(@TempDir Path tempDir) throws IOException {
    // 兩個有內容的 repo 資料夾（測遞迴刪除），加上一個應該保留的 .gitkeep
    Path repoA = Files.createDirectories(tempDir.resolve("repoA/src"));
    Files.writeString(repoA.resolve("Main.java"), "class Main {}");
    Path repoB = Files.createDirectories(tempDir.resolve("repoB"));
    Path gitKeep = Files.createFile(tempDir.resolve(".gitkeep"));

    MyPostConstruct.path = tempDir.toString();
    myPostConstruct.deleteCloneFile();

    assertFalse(Files.exists(tempDir.resolve("repoA")), "repoA 資料夾應該被刪除");
    assertFalse(Files.exists(repoB), "repoB 資料夾應該被刪除");
    assertTrue(Files.exists(gitKeep), "一般檔案不該被刪除");
    assertEquals(1, tempDir.toFile().list().length, "只應剩下 .gitkeep");
  }

  @Test
  @DisplayName("測試 deleteProjectDatBase()")
  void deleteProjectDatBaseTest() {
    myPostConstruct.deleteProjectDatBase();
  }

  @Test
  @DisplayName("addSuperAccount() 交由 UserService 判斷是否建立")
  void addSuperAccountDelegatesTest() {
    myPostConstruct.addSuperAccount();
    Mockito.verify(userService).addSuperAccount();
  }

  @Test
  @DisplayName("測試 initialSetting() 會建立超級帳號")
  void initialSettingTest() throws IllegalAccessException {
    myPostConstruct.initialSettings();
    Mockito.verify(userService).addSuperAccount();
  }
}
