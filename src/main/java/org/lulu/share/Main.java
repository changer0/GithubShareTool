package org.lulu.share;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        Git git = GitUtil.open(new File("C:\\Users\\zll88\\IdeaProjects\\GithubShareUtil"));
        String un = KVStorage.get("un", "");
        String pwd = KVStorage.get("pwd", "");
        CredentialsProvider cp = GitUtil.createCredential(un, pwd);
        GitUtil.commit(git, "测试", cp);
//        KVStorage.put("un", "123");
//        KVStorage.put("pwd", "123");
    }
}
