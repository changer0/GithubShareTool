package org.lulu.share;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class GitHelper {
    private final Git git;

    private CredentialsProvider provider;

    Repository repository;


    public Git getGit() {
        return git;
    }

    public GitHelper(File file, CredentialsProvider provider) throws IOException {
        git = Git.open(file);
        this.provider = provider;
        repository = git.getRepository();
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
        return Git.cloneRepository()
                .setCredentialsProvider(provider)
                .setURI(repoUrl)
                .setDirectory(new File(cloneDir)).call();
    }

    public void pull() throws GitAPIException {
        git.pull().call();
    }

    public void commit(String message) throws GitAPIException {
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
}
