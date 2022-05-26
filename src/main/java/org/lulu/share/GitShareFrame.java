package org.lulu.share;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class GitShareFrame extends JFrame {

    public static final int PADDING = 10;

    public static final int W = 500;

    public static final int H = 500;

    private final LogPanel log = new LogPanel();

    private final FileList fileList = new FileList();


    /**
     * 当前 Git 路径
     */
    private String curGitPath;

    private GitHelper gitHelper;

    private CredentialsProvider credentialsProvider;
    private JLabel gitPathLabel;

    public GitShareFrame() throws HeadlessException {
        super("GitHub 分享工具");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //禁止调整大小
        setResizable(false);
        setSize(W, H);
        // 居中
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - getWidth()) / 2;
        int y = (screenSize.height - getHeight()) / 2;
        setLocation(x, y);
        //绝对布局
        setLayout(null);
        addContainer();
        createGitHelper(KVStorage.get("git_path", ""));
        //必须最后调用
        setVisible(true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // 基础逻辑控制
    ///////////////////////////////////////////////////////////////////////////

    private void createCredential() {
        if (credentialsProvider != null) {
            return;
        }
        String un = KVStorage.get("un", "");
        String pwd = KVStorage.get("pwd", "");
        if (StringUtils.isAnyEmpty(un, pwd)) {
            log.e("请配置 GitHub 用户名密码!");
            return;
        }
        log.i("用户名密码配置成功");
        credentialsProvider = new UsernamePasswordCredentialsProvider(un, pwd);
    }

    private void createGitHelper(String gitPath) {
        curGitPath = gitPath;
        KVStorage.put("git_path", gitPath);
        refreshCurGitPath();
        createCredential();
        if (!StringUtils.isEmpty(curGitPath) && credentialsProvider != null) {
            try {
                gitHelper = new GitHelper(new File(curGitPath), credentialsProvider);
                log.i("仓库配置成功: " + curGitPath);
            } catch (IOException e) {
                log.e("请确保该目录为 Git 工程!");
            }
        } else {
            log.e("未配置 Git 仓库, 请点击【仓库配置】");
        }
    }





    ///////////////////////////////////////////////////////////////////////////
    // UI 控制
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 添加内容
     */
    private void addContainer() {
        addTop();
        addList();
        addRightController();
        addBottomLog();
    }


    /**
     * 顶部
     */
    private void addTop() {
        JButton back = new JButton("返回");
        back.setBounds(PADDING, PADDING, 60, 20);
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                fileList.returnDir();
            }
        });
        gitPathLabel = new JLabel();
        gitPathLabel.setBounds(80, PADDING, W - 100, 20);
        add(back);
        add(gitPathLabel);
    }

    private void refreshCurGitPath() {
        gitPathLabel.setText("当前仓库地址: " + curGitPath);
    }

    /**
     * 文件列表
     */
    private void addList() {
        JPanel jPanel = new JPanel();
        jPanel.setTransferHandler(SelectFileUtil.createDragTransferHandler(selectedFile -> {
            createGitHelper(selectedFile.getAbsolutePath());
            fileList.openItem(selectedFile);
        }));

        JLabel jTextField = new JLabel();
        jTextField.setBounds(80, PADDING, W - 200, 20);

        fileList.setFileOpenListener(nowDir -> {
            jTextField.setText(nowDir.getAbsolutePath());
        });



        jPanel.setBounds(PADDING, 40, W - 130, H - 200);
        jPanel.setLayout(new BorderLayout(2, 5));
        jPanel.setBackground(Color.WHITE);
        JScrollPane comp = new JScrollPane(fileList);

        fileList.openItem(new File("C:\\Users\\zll88\\IdeaProjects\\GithubShareUtil"));
        jPanel.add(jTextField, BorderLayout.NORTH);
        jPanel.add(comp);
        add(jPanel);
    }

    /**
     * 右侧控制区
     */
    private void addRightController() {
        int w = 110;
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout());
        jPanel.setBounds(W - w, PADDING + 20, w - 20, H - 60);
//        jPanel.setBackground(Color.RED);
        Box box = Box.createVerticalBox();

        JButton repoConfig = new JButton("仓库配置");
        repoConfig.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                SelectFileUtil.selectDir(curGitPath, selectedFile -> {
                    createGitHelper(selectedFile.getAbsolutePath());
                    fileList.openItem(selectedFile);
                });
            }
        });
        box.add(repoConfig);

        box.add(Box.createVerticalStrut(10));
        JButton clear = new JButton("清除日志");
        clear.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                log.clear();
            }
        });
        box.add(clear);

        jPanel.add(box);
        add(jPanel);
    }

    /**
     * 底部日志区
     */
    private void addBottomLog() {
        int h = 100;
        log.setBounds(PADDING, H - h - 50, W - 130, h);
        add(log);
    }

}
