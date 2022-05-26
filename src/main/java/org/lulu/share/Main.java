package org.lulu.share;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        Git git = GitUtil.open(new File("C:\\Users\\zll88\\IdeaProjects\\GithubShareUtil"));
        String un = KVStorage.get("un", "");
        String pwd = KVStorage.get("pwd", "");
        CredentialsProvider cp = GitUtil.createCredential(un, pwd);
        //GitUtil.commit(git, "测试", cp);
//        GitUtil.push(git, cp);
//        KVStorage.put("un", "123");
//        KVStorage.put("pwd", "123");
        new GitShareFrame();
        Repository repository = git.getRepository();
        String url = repository.getConfig().getString("remote", "origin", "url").replace(".git", "");
        System.out.println("url: " + url);
        System.out.println(git);
        boolean isPushed = repository.resolve("origin/master").equals(repository.resolve("master"));
        System.out.println(isPushed);
    }
}
