package com.oj.service;

import com.oj.dto.MaliciousCodeDetectionResult;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class MaliciousCodeDetector {

    private static final String MALICIOUS_CODE_ERROR = "检测到潜在恶意代码，请检查代码内容";

    public MaliciousCodeDetectionResult detect(String code, String language) {
        if (code == null || code.isEmpty()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(true)
                    .message("代码为空")
                    .build();
        }

        String lowerCode = code.toLowerCase();
        String lowerLanguage = language.toLowerCase();

        if (lowerLanguage.contains("java")) {
            return detectJavaMalicious(code, lowerCode);
        } else if (lowerLanguage.contains("python")) {
            return detectPythonMalicious(code, lowerCode);
        } else if (lowerLanguage.contains("c++") || lowerLanguage.contains("cpp")) {
            return detectCppMalicious(code, lowerCode);
        } else if (lowerLanguage.contains("javascript") || lowerLanguage.contains("js")) {
            return detectJavaScriptMalicious(code, lowerCode);
        } else if (lowerLanguage.contains("go")) {
            return detectGoMalicious(code, lowerCode);
        } else if (lowerLanguage.contains("c")) {
            return detectCMalicious(code, lowerCode);
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }

    private MaliciousCodeDetectionResult detectJavaMalicious(String code, String lowerCode) {
        if (Pattern.compile("runtime\\.getruntime\\(\\)\\.exec\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：Runtime.exec()")
                    .build();
        }

        if (Pattern.compile("processbuilder\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：ProcessBuilder")
                    .build();
        }

        if (Pattern.compile("class\\.forname\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到反射调用：Class.forName()")
                    .build();
        }

        if (Pattern.compile("method\\.invoke\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到反射调用：Method.invoke()")
                    .build();
        }

        if (Pattern.compile("file\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件操作：File")
                    .build();
        }

        if (Pattern.compile("files\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件操作：Files")
                    .build();
        }

        if (Pattern.compile("printwriter\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件写入：PrintWriter")
                    .build();
        }

        if (Pattern.compile("fileoutputstream\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件写入：FileOutputStream")
                    .build();
        }

        if (Pattern.compile("url\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到网络请求：URL")
                    .build();
        }

        if (Pattern.compile("httpurlconnection\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到网络请求：HttpURLConnection")
                    .build();
        }

        if (Pattern.compile("for\\s*\\(\\s*;\\s*;\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("while\\s*\\(true\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("new\\s+byte\\s*\\[\\s*\\d+\\s*\\]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的内存攻击")
                    .build();
        }

        if (Pattern.compile("system\\s*\\.gc\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统GC调用")
                    .build();
        }

        if (Pattern.compile("system\\.exit\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统退出调用")
                    .build();
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }

    private MaliciousCodeDetectionResult detectPythonMalicious(String code, String lowerCode) {
        if (Pattern.compile("os\\.system\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：os.system()")
                    .build();
        }

        if (Pattern.compile("subprocess\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：subprocess")
                    .build();
        }

        if (Pattern.compile("popen\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：popen")
                    .build();
        }

        if (Pattern.compile("open\\s*\\(.*['\"]\\w['\"]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件写入：open(..., 'w')")
                    .build();
        }

        if (Pattern.compile("urllib\\s*\\.")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到网络请求：urllib")
                    .build();
        }

        if (Pattern.compile("requests\\s*\\.")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到网络请求：requests")
                    .build();
        }

        if (Pattern.compile("http\\s*\\.")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到网络请求：http")
                    .build();
        }

        if (Pattern.compile("eval\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到动态执行：eval()")
                    .build();
        }

        if (Pattern.compile("exec\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到动态执行：exec()")
                    .build();
        }

        if (Pattern.compile("compile\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到动态编译：compile()")
                    .build();
        }

        if (Pattern.compile("while\\s+true\\s*:")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("for\\s+\\w+\\s+in\\s+range\\(\\d+\\)\\s*:")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("\\[\\s*\\]\\s*\\*\\s*\\d+\\s*\\]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的内存攻击")
                    .build();
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }

    private MaliciousCodeDetectionResult detectCppMalicious(String code, String lowerCode) {
        if (Pattern.compile("system\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：system()")
                    .build();
        }

        if (Pattern.compile("popen\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：popen")
                    .build();
        }

        if (Pattern.compile("exec\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：exec()")
                    .build();
        }

        if (Pattern.compile("fopen\\s*\\(.*['\"]\\w['\"]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件写入：fopen(..., 'w')")
                    .build();
        }

        if (Pattern.compile("system\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：system()")
                    .build();
        }

        if (Pattern.compile("while\\s*\\(true\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("for\\s*\\(\\s*;\\s*;\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("new\\s+char\\s*\\[\\s*\\d+\\s*\\]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的内存攻击")
                    .build();
        }

        if (Pattern.compile("malloc\\s*\\(\\s*\\d+\\s*\\*\\s*\\d+\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的内存攻击")
                    .build();
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }

    private MaliciousCodeDetectionResult detectJavaScriptMalicious(String code, String lowerCode) {
        if (Pattern.compile("eval\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到动态执行：eval()")
                    .build();
        }

        if (Pattern.compile("function\\s*\\(\\s*['\"]\\w['\"]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到动态执行：Function()")
                    .build();
        }

        if (Pattern.compile("require\\s*\\(['\"]fs['\"]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件系统操作：require('fs')")
                    .build();
        }

        if (Pattern.compile("child_process\\s*\\.")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程操作：child_process")
                    .build();
        }

        if (Pattern.compile("exec\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程执行：exec()")
                    .build();
        }

        if (Pattern.compile("spawn\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：spawn()")
                    .build();
        }

        if (Pattern.compile("while\\s*\\(true\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("for\\s*\\(\\s*;\\s*;\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("new\\s+array\\s*\\(\\s*\\d+\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的内存攻击")
                    .build();
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }

    private MaliciousCodeDetectionResult detectGoMalicious(String code, String lowerCode) {
        if (Pattern.compile("exec\\.command\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：exec.Command()")
                    .build();
        }

        if (Pattern.compile("os\\.exec\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：os.Exec()")
                    .build();
        }

        if (Pattern.compile("os\\.startprocess\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：os.StartProcess()")
                    .build();
        }

        if (Pattern.compile("ioutil\\s*\\.")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件操作：ioutil")
                    .build();
        }

        if (Pattern.compile("http\\.get\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到网络请求：http.Get()")
                    .build();
        }

        if (Pattern.compile("net\\s*\\.")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到网络操作：net")
                    .build();
        }

        if (Pattern.compile("reflect\\s*\\.")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到反射操作：reflect")
                    .build();
        }

        if (Pattern.compile("for\\s*\\{\\s*;\\s*;\\s*\\}")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("make\\s*\\(\\s*\\[\\s*\\d+\\s*\\]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的内存攻击")
                    .build();
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }

    private MaliciousCodeDetectionResult detectCMalicious(String code, String lowerCode) {
        if (Pattern.compile("system\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：system()")
                    .build();
        }

        if (Pattern.compile("popen\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程创建：popen()")
                    .build();
        }

        if (Pattern.compile("exec\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到进程执行：exec()")
                    .build();
        }

        if (Pattern.compile("fopen\\s*\\(.*['\"]\\w['\"]")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到文件写入：fopen(..., 'w')")
                    .build();
        }

        if (Pattern.compile("system\\s*\\(")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到系统命令执行：system()")
                    .build();
        }

        if (Pattern.compile("while\\s*\\(1\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("for\\s*\\(\\s*;\\s*;\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的无限循环")
                    .build();
        }

        if (Pattern.compile("malloc\\s*\\(\\s*\\d+\\s*\\*\\s*\\d+\\s*\\)")
                .matcher(lowerCode).find()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("检测到可能的内存攻击")
                    .build();
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }
}