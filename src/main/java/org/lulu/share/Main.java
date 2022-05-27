package org.lulu.share;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {

        String un = KVStorage.get("un", "");
        String pwd = KVStorage.get("pwd", "");
        CredentialsProvider cp = GitUtil.createCredential(un, pwd);
        //GitUtil.commit(git, "测试", cp);
//        GitUtil.push(git, cp);
//        KVStorage.put("un", "123");
//        KVStorage.put("pwd", "123");
        new GitShareFrame();

        GitHelper gitHelper = new GitHelper(new File("C:\\OtherProject\\ShareDoc"));
        gitHelper.setProvider(cp);
        List<String> remoteBranch = gitHelper.getRemoteBranch();
        List<String> localBranch = gitHelper.getLocalBranch();
        System.out.println("远程:" + remoteBranch);
        System.out.println("本地:" + localBranch);
    }
}
