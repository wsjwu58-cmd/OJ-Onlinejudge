package com.oj.judge.hack;

import com.alibaba.fastjson.JSONObject;
import com.oj.judge.config.Judge0Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

@Slf4j
@Component
public class ValidatorRunner {

    @Autowired
    private Judge0Client judge0Client;

    private static final int CXX_LANG_ID = 54;
    private static final float VALIDATOR_TIMEOUT_SEC = 5.0f;
    private static final int VALIDATOR_MEMORY_KB = 256 * 1024;

    public ValidatorResult validate(String validatorPath, String hackInput) {
        Path sourceFile = Paths.get(validatorPath);
        if (!Files.exists(sourceFile)) {
            log.warn("Validator 文件不存在: {}", validatorPath);
            return ValidatorResult.invalid("Validator 文件不存在: " + validatorPath);
        }
        return runInJudge0Sandbox(validatorPath, hackInput);
    }

    public ValidatorResult validate(String validatorPath, String validatorExePath,
                                      String cachedHash, String hackInput) {
        Path sourceFile = Paths.get(validatorPath);
        if (!Files.exists(sourceFile)) {
            return ValidatorResult.invalid("Validator 源码不存在: " + validatorPath);
        }

        String currentHash = sha256(sourceFile);
        Path exeFile = Paths.get(validatorExePath);

        if (Files.exists(exeFile) && currentHash.equals(cachedHash)) {
            log.info("Validator 编译缓存命中: {}", validatorExePath);
            return runInJudge0Sandbox(validatorPath, hackInput);
        }

        log.info("Validator 编译缓存未命中，通过 Judge0 重新编译");
        return runInJudge0Sandbox(validatorPath, hackInput);
    }

    private ValidatorResult runInJudge0Sandbox(String validatorPath, String hackInput) {
        String cppCode;
        try {
            cppCode = Files.readString(Paths.get(validatorPath));
        } catch (Exception e) {
            log.error("读取 Validator 文件失败: {}", validatorPath, e);
            return ValidatorResult.invalid("读取 Validator 文件失败: " + e.getMessage());
        }

        try {
            JSONObject result = judge0Client.submitAndWait(
                    cppCode, CXX_LANG_ID,
                    hackInput,
                    null,
                    VALIDATOR_TIMEOUT_SEC,
                    VALIDATOR_MEMORY_KB
            );

            JSONObject status = result.getJSONObject("status");
            int statusId = status.getIntValue("id");

            if (statusId == 3) {
                return ValidatorResult.valid(judge0Client.decodeField(result, "stdout"));
            } else if (statusId == 6) {
                String compileErr = judge0Client.decodeField(result, "compile_output");
                return ValidatorResult.invalid("Validator 编译失败: " + compileErr);
            } else {
                String reason = judge0Client.decodeField(result, "stderr");
                if (reason == null || reason.isEmpty()) {
                    reason = judge0Client.decodeField(result, "stdout");
                }
                return ValidatorResult.invalid(
                        "Validator 校验不通过(status=" + statusId + "): " + reason);
            }
        } catch (Exception e) {
            log.error("Validator Judge0 运行异常", e);
            return ValidatorResult.invalid("Validator 运行异常: " + e.getMessage());
        }
    }

    public static String sha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidatorResult {
        private boolean valid;
        private int exitCode;
        private String stdout;
        private String stderr;

        public static ValidatorResult valid(String stdout) {
            return new ValidatorResult(true, 0, stdout != null ? stdout : "", "");
        }

        public static ValidatorResult invalid(String reason) {
            return new ValidatorResult(false, -1, "", reason);
        }
    }
}
