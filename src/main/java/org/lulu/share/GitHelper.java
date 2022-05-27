package org.lulu.share;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GitHelper {
    private final Git git;

    private CredentialsProvider provider;

    Repository repository;


    public Git getGit() {
        return git;
    }

    public GitHelper(File file) throws IOException {
        git = Git.open(file);
        repository = git.getRepository();
    }



    public void setProvider(CredentialsProvider provider) {
        this.provider = provider;
    }

    public static CredentialsProvider createCredential(String userName, String password) {
        return new UsernamePasswordCredentialsProvider(userName, password);
    }


    public Repository getRepository() {
        return repository;
    }


    public Repository getRepositoryFromDir(String dir) throws IOException {
        return new FileRepositoryBuilder()
                .setGitDir(Paths.get(dir, ".git").toFile())
                .build();
    }

    public static Git open(File file) throws IOException {
        return Git.open(file);
    }


    public Git fromCloneRepository(String repoUrl, String cloneDir) throws GitAPIException {
        if (provider == null) {
            throw new RuntimeException("未配置令牌");
        }
        return Git.cloneRepository()
                .setCredentialsProvider(provider)
                .setURI(repoUrl)
                .setDirectory(new File(cloneDir)).call();
    }

    public void pull() throws GitAPIException {
        git.pull().call();
    }

    public void commit(String message) throws GitAPIException {
        for (String s : status().getMissing()) {
            git.rm().addFilepattern(s).call();
        }
        git.add().addFilepattern(".").call();
        git.commit()
                .setMessage(message)
                .call();
    }

    public void push() throws GitAPIException, IOException {
        push(null);
    }

    public void push(String branch) throws GitAPIException, IOException {
        if (branch == null) {
            branch = git.getRepository().getBranch();
        }
        if (provider == null) {
            throw new RuntimeException("未配置令牌");
        }
        git.push()
                .setCredentialsProvider(provider)
                .setRemote("origin").setRefSpecs(new RefSpec(branch)).call();
    }

    public String getRemoteUrl() {
        return repository.getConfig().getString("remote", "origin", "url").replace(".git", "");
    }

    public Status status() throws GitAPIException {
        return git.status().call();
    }

    public List<String> getRemoteBranch() throws GitAPIException {
        List<String> branchNameList = new ArrayList<>();
        for (Ref ref : git.lsRemote().call()) {
            String refName = ref.getName();
            if (refName.startsWith("refs/heads/")) {                       //需要进行筛选
                String branchName = refName.replace("refs/heads/", "");
                branchNameList.add(branchName);
            }
        }
        return branchNameList;
    }

    public List<String> getLocalBranch() throws GitAPIException {
        List<String> branchNameList = new ArrayList<>();
        for (Ref ref : git.branchList().call()) {
            String refName = ref.getName();
            if (refName.startsWith("refs/heads/")) {                       //需要进行筛选
                String branchName = refName.replace("refs/heads/", "");
                branchNameList.add(branchName);
            }
        }
        return branchNameList;
    }

    /**
     * 远程 本地
     *  1   0  √
     *  0   0  √
     *  0   1  √
      * 1   1  √
     * @param branch
     * @throws GitAPIException
     */
    public void createBranchWithRemote(String branch) throws GitAPIException {

        boolean isRemote = getRemoteBranch().contains(branch);
        boolean isLocal = getLocalBranch().contains(branch);
        System.out.println("远程是否存在: " + isRemote);
        System.out.println("本地是否存在: " + isLocal);

        git.fetch();

        //远程存在
        if (isRemote) {
            //本地存在
            if (isLocal) {
                git.checkout().setCreateBranch(false).setName(branch).call();
            } else {
                //本地不存在
                git.checkout().setCreateBranch(true).setName(branch).setStartPoint("origin/" + branch).call();
            }
            //拉取最新的提交
            git.pull().call();
        } else {
            Ref ref;
            //远程分支不存在, 创建
            if (isLocal) {
                ref = git.checkout().setCreateBranch(false).setName(branch).call();
            } else {
                //本地不存在
                ref = git.branchCreate().setName(branch).call();
            }
            git.push().add(ref).setCredentialsProvider(provider).call();

            //切到当前分支
            git.checkout().setCreateBranch(false).setName(branch).call();

        }
    }
}
