package org.lulu.share;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;

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
        //删除本地分支
        //gitHelper.getGit().branchDelete().setForce(true).setBranchNames("dev5").call();
//        gitHelper.removeBranch("dev5");
//        RefSpec refSpec = new RefSpec()
//                .setSource(null)
//                .setDestination("refs/heads/dev9");
//
//        gitHelper.getGit().push().setCredentialsProvider(cp).setRefSpecs(refSpec).setRemote("origin").call();

//        System.out.println("远程:" + remoteBranch);
//        System.out.println("本地:" + localBranch);
    }
}
