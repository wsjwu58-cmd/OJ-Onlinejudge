package com.oj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class MaliciousCodeRulesConfig {

    @Bean
    public MaliciousCodeDetectionConfig maliciousCodeDetectionConfig() {
        MaliciousCodeDetectionConfig config = new MaliciousCodeDetectionConfig();
        config.setEnabled(true);
        config.setLogMaliciousSubmissions(true);
        config.setMaxCodeLength(100000);
        
        Map<String, MaliciousCodeDetectionConfig.LanguageRules> languageRules = new HashMap<>();
        
        languageRules.put("java", createJavaRules());
        languageRules.put("python", createPythonRules());
        languageRules.put("cpp", createCppRules());
        languageRules.put("c++", createCppRules());
        languageRules.put("javascript", createJavaScriptRules());
        languageRules.put("js", createJavaScriptRules());
        languageRules.put("go", createGoRules());
        languageRules.put("c", createCRules());
        
        config.setLanguageRules(languageRules);
        
        return config;
    }

    private MaliciousCodeDetectionConfig.LanguageRules createJavaRules() {
        MaliciousCodeDetectionConfig.LanguageRules rules = new MaliciousCodeDetectionConfig.LanguageRules();
        
        List<MaliciousCodeDetectionConfig.DetectionRule> ruleList = new ArrayList<>();
        
        ruleList.add(createRule("系统命令执行", "\\bruntime\\.getruntime\\(\\)\\.exec\\s*\\(", 
            "检测到系统命令执行：Runtime.exec()", 3, true));
        ruleList.add(createRule("进程创建", "\\bprocessbuilder\\s*\\(", 
            "检测到进程创建：ProcessBuilder", 3, true));
        ruleList.add(createRule("反射调用", "\\bclass\\.forname\\s*\\(", 
            "检测到反射调用：Class.forName()", 2, true));
        ruleList.add(createRule("文件操作", "\\b(new\\s+)?(file|files|printwriter|fileoutputstream|filewriter|bufferedwriter)\\s*\\(", 
            "检测到文件操作", 2, true));
        ruleList.add(createRule("网络请求", "\\b(new\\s+)?(url|httpurlconnection|httpsurlconnection|socket|serversocket)\\s*\\(", 
            "检测到网络请求", 2, true));
        ruleList.add(createRule("无限循环", "\\b(for\\s*\\(\\s*;\\s*;\\s*\\)|while\\s*\\(true\\s*\\))", 
            "检测到可能的无限循环", 1, true));
        ruleList.add(createRule("系统退出", "\\bsystem\\.(exit|halt)\\s*\\(", 
            "检测到系统退出调用", 2, true));
        
        rules.setRules(ruleList);
        
        List<String> whitelist = new ArrayList<>();
        whitelist.add("println\\s*\\(");
        whitelist.add("print\\s*\\(");
        whitelist.add("printf\\s*\\(");
        whitelist.add("format\\s*\\(");
        
        rules.setWhitelist(whitelist);
        
        return rules;
    }

    private MaliciousCodeDetectionConfig.LanguageRules createPythonRules() {
        MaliciousCodeDetectionConfig.LanguageRules rules = new MaliciousCodeDetectionConfig.LanguageRules();
        
        List<MaliciousCodeDetectionConfig.DetectionRule> ruleList = new ArrayList<>();
        
        ruleList.add(createRule("系统命令执行", "\\bos\\.system\\s*\\(", 
            "检测到系统命令执行：os.system()", 3, true));
        ruleList.add(createRule("进程创建", "\\b(subprocess|popen)\\s*\\(", 
            "检测到进程创建", 3, true));
        ruleList.add(createRule("文件写入", "\\bopen\\s*\\([^)]*['\"]\\w['\"]", 
            "检测到文件写入：open(..., 'w')", 2, true));
        ruleList.add(createRule("网络请求", "\\b(urllib|requests|http)\\s*\\.", 
            "检测到网络请求", 2, true));
        ruleList.add(createRule("动态执行", "\\b(eval|exec|compile)\\s*\\(", 
            "检测到动态执行", 3, true));
        ruleList.add(createRule("无限循环", "\\b(while\\s+true\\s*:|for\\s+\\w+\\s+in\\s+range\\(\\d+\\)\\s*:)", 
            "检测到可能的无限循环", 1, true));
        
        rules.setRules(ruleList);
        
        List<String> whitelist = new ArrayList<>();
        whitelist.add("print\\s*\\(");
        whitelist.add("input\\s*\\(");
        whitelist.add("range\\s*\\(");
        
        rules.setWhitelist(whitelist);
        
        return rules;
    }

    private MaliciousCodeDetectionConfig.LanguageRules createCppRules() {
        MaliciousCodeDetectionConfig.LanguageRules rules = new MaliciousCodeDetectionConfig.LanguageRules();
        
        List<MaliciousCodeDetectionConfig.DetectionRule> ruleList = new ArrayList<>();
        
        ruleList.add(createRule("系统命令执行", "\\bsystem\\s*\\(", 
            "检测到系统命令执行：system()", 3, true));
        ruleList.add(createRule("进程创建", "\\b(popen|exec)\\s*\\(", 
            "检测到进程创建", 3, true));
        ruleList.add(createRule("文件操作", "\\b(fopen|fwrite|fprintf|fputs)\\s*\\(", 
            "检测到文件操作", 2, true));
        ruleList.add(createRule("无限循环", "\\b(while\\s*\\(true\\s*\\)|for\\s*\\(\\s*;\\s*;\\s*\\))", 
            "检测到可能的无限循环", 1, true));
        ruleList.add(createRule("内存分配", "\\b(new\\s+char\\s*\\[\\s*\\d+\\s*\\]|malloc\\s*\\(\\s*\\d+\\s*\\*\\s*\\d+\\s*\\))", 
            "检测到可能的内存攻击", 2, true));
        
        rules.setRules(ruleList);
        
        List<String> whitelist = new ArrayList<>();
        whitelist.add("cout\\s*<<");
        whitelist.add("cin\\s*>>");
        whitelist.add("printf\\s*\\(");
        whitelist.add("scanf\\s*\\(");
        
        rules.setWhitelist(whitelist);
        
        return rules;
    }

    private MaliciousCodeDetectionConfig.LanguageRules createJavaScriptRules() {
        MaliciousCodeDetectionConfig.LanguageRules rules = new MaliciousCodeDetectionConfig.LanguageRules();
        
        List<MaliciousCodeDetectionConfig.DetectionRule> ruleList = new ArrayList<>();
        
        ruleList.add(createRule("动态执行", "\\b(eval|Function\\s*\\()\\s*\\(", 
            "检测到动态执行", 3, true));
        ruleList.add(createRule("文件系统", "\\brequire\\s*\\(\\s*['\"]fs['\"]", 
            "检测到文件系统操作", 2, true));
        ruleList.add(createRule("进程操作", "\\bchild_process\\s*\\.", 
            "检测到进程操作", 3, true));
        ruleList.add(createRule("无限循环", "\\b(while\\s*\\(true\\s*\\)|for\\s*\\(\\s*;\\s*;\\s*\\))", 
            "检测到可能的无限循环", 1, true));
        
        rules.setRules(ruleList);
        
        List<String> whitelist = new ArrayList<>();
        whitelist.add("console\\.log\\s*\\(");
        whitelist.add("console\\.error\\s*\\(");
        whitelist.add("alert\\s*\\(");
        
        rules.setWhitelist(whitelist);
        
        return rules;
    }

    private MaliciousCodeDetectionConfig.LanguageRules createGoRules() {
        MaliciousCodeDetectionConfig.LanguageRules rules = new MaliciousCodeDetectionConfig.LanguageRules();
        
        List<MaliciousCodeDetectionConfig.DetectionRule> ruleList = new ArrayList<>();
        
        ruleList.add(createRule("系统命令执行", "\\bexec\\.Command\\s*\\(", 
            "检测到系统命令执行：exec.Command()", 3, true));
        ruleList.add(createRule("进程创建", "\\b(os\\.Exec|os\\.StartProcess)\\s*\\(", 
            "检测到进程创建", 3, true));
        ruleList.add(createRule("文件操作", "\\bioutil\\s*\\.", 
            "检测到文件操作", 2, true));
        ruleList.add(createRule("网络请求", "\\b(http\\.Get|net\\.)\\s*\\(", 
            "检测到网络请求", 2, true));
        ruleList.add(createRule("反射操作", "\\breflect\\s*\\.", 
            "检测到反射操作", 2, true));
        ruleList.add(createRule("无限循环", "\\bfor\\s*\\{\\s*;\\s*;\\s*\\}", 
            "检测到可能的无限循环", 1, true));
        
        rules.setRules(ruleList);
        
        List<String> whitelist = new ArrayList<>();
        whitelist.add("fmt\\.Print");
        whitelist.add("fmt\\.Scan");
        
        rules.setWhitelist(whitelist);
        
        return rules;
    }

    private MaliciousCodeDetectionConfig.LanguageRules createCRules() {
        MaliciousCodeDetectionConfig.LanguageRules rules = new MaliciousCodeDetectionConfig.LanguageRules();
        
        List<MaliciousCodeDetectionConfig.DetectionRule> ruleList = new ArrayList<>();
        
        ruleList.add(createRule("系统命令执行", "\\bsystem\\s*\\(", 
            "检测到系统命令执行：system()", 3, true));
        ruleList.add(createRule("进程创建", "\\b(popen|exec)\\s*\\(", 
            "检测到进程创建", 3, true));
        ruleList.add(createRule("文件操作", "\\b(fopen|fwrite|fprintf|fputs)\\s*\\(", 
            "检测到文件操作", 2, true));
        ruleList.add(createRule("无限循环", "\\b(while\\s*\\(1\\s*\\)|for\\s*\\(\\s*;\\s*;\\s*\\))", 
            "检测到可能的无限循环", 1, true));
        ruleList.add(createRule("内存分配", "\\bmalloc\\s*\\(\\s*\\d+\\s*\\*\\s*\\d+\\s*\\)", 
            "检测到可能的内存攻击", 2, true));
        
        rules.setRules(ruleList);
        
        List<String> whitelist = new ArrayList<>();
        whitelist.add("printf\\s*\\(");
        whitelist.add("scanf\\s*\\(");
        
        rules.setWhitelist(whitelist);
        
        return rules;
    }

    private MaliciousCodeDetectionConfig.DetectionRule createRule(String name, String pattern, String description, int severity, boolean enabled) {
        MaliciousCodeDetectionConfig.DetectionRule rule = new MaliciousCodeDetectionConfig.DetectionRule();
        rule.setName(name);
        rule.setPattern(pattern);
        rule.setDescription(description);
        rule.setSeverity(severity);
        rule.setEnabled(enabled);
        return rule;
    }
}
