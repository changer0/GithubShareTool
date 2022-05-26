package org.lulu.share;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

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

    private volatile boolean isReleasing = false;

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

        if (!StringUtils.isEmpty(curGitPath)) {
            fileList.openItem(new File(curGitPath));
        }
    }


    private void copyShareLink(File file) {
        if (isReleasing) {
            return;
        }
        if (gitHelper == null) {
            return;
        }
        if (file.isDirectory()) {
            log.e("请选择文件!");
            return;
        }
        String remoteUrl = gitHelper.getRemoteUrl();
        if (StringUtils.isEmpty(remoteUrl)) {
            log.e("未关联远程仓库!请先关联");
            return;
        }
        String filePath = file.getAbsolutePath();
        if (!filePath.startsWith(curGitPath)) {
            log.e("请在选中的 Git 仓库内进行!");
            return;
        }
        //tryAutoRelease();
        if (checkNeedRelease()) {
            log.e("暂未发布, 请点击【一键发布】");
            return;
        }
        //查找相对路径!
        ArrayList<String> pathList = new ArrayList<>();
        File tFile = file;
        do {
            pathList.add(0, tFile.getName());
            tFile = tFile.getParentFile();
        } while (!StringUtils.equals(tFile.getAbsolutePath(), curGitPath));

        StringBuilder relativePath = new StringBuilder();
        for (String s : pathList) {
            relativePath.append("/").append(URLEncoder.encode(s).replace("+", "%20"));
        }

        String htmlPreviewPrefix = "https://htmlpreview.github.io/?";
        String result = htmlPreviewPrefix + remoteUrl + "/blob/master" + relativePath;
        //log.i("相对路径:" + t);
        ClipUtil.setSysClipboardText(result);
        log.i("成功复制到剪切板: " + result);
    }


    private void tryAutoRelease() {
        log.i("尝试自动发布");
        if (checkNeedRelease()) {
            oneKeyRelease();
        } else {
            log.i("无需发布");
        }
    }

    private void oneKeyRelease() {
        if (isReleasing) {
            return;
        }
        isReleasing = true;
        TaskHandler.getInstance().enqueue(() -> {
            try {
                log.i("开始发布");
                log.i("开始拉取...");
                gitHelper.pull();
                log.i("开始提交...");
                gitHelper.commit("commit: " + System.currentTimeMillis());
                log.i("开始推送...");
                gitHelper.push();
                log.i("发布完成");
            } catch (GitAPIException | IOException e) {
                log.e("git 发生错误: " + e.getMessage());
            }
            isReleasing = false;
        });

    }

    private boolean checkNeedRelease() {
        Repository repository = gitHelper.getRepository();
        try {
            // 1. 检查是否提交
            if (!repository.resolve("origin/master").equals(repository.resolve("master"))) {
                return true;
            }
            //2. 检查是否有未 add commit 的
            Status status = gitHelper.status();
            if (!gitHelper.status().isClean()) {
                return true;
            }
        } catch (IOException | GitAPIException e) {
            log.e("git 发生错误: " + e.getMessage());
        }
        return false;
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
        }));

        JLabel jTextField = new JLabel();
        jTextField.setBounds(80, PADDING, W - 200, 20);

        fileList.setFileOpenListener(nowDir -> {
            jTextField.setText(nowDir.getAbsolutePath());
        });

        fileList.setFileRightSelectedListener((e, file) -> {
            JPopupMenu jPopupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("复制分享链接");
            menuItem.addActionListener(e1 -> copyShareLink(file));
            jPopupMenu.add(menuItem);
            jPopupMenu.show(fileList, e.getX(), e.getY());
        });

        jPanel.setBounds(PADDING, 40, W - 130, H - 200);
        jPanel.setLayout(new BorderLayout(2, 5));
        jPanel.setBackground(Color.WHITE);
        JScrollPane comp = new JScrollPane(fileList);

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
        addRightButton(box, "返回仓库", event -> {
            if (StringUtils.isEmpty(curGitPath)) {
                log.e("请配置仓库");
                return;
            }
            fileList.openItem(new File(curGitPath));
        });

        addRightButton(box, "仓库配置", event -> {
//            SelectFileUtil.selectDir(curGitPath, selectedFile -> {
//                createGitHelper(selectedFile.getAbsolutePath());
//            });
            showGitRepoConfigDialog();
        });

        addRightButton(box, "一键发布", event -> {
            oneKeyRelease();
        });

        addRightButton(box, "刷         新", event -> {
            fileList.refresh();
        });
        addRightButton(box, "清除日志", event -> {
            log.clear();
        });

        jPanel.add(box);
        add(jPanel);
    }

    public void showGitRepoConfigDialog() {
        JDialog dialog=new JDialog(this, "仓库配置",true);
        dialog.setSize(202, 100);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - dialog.getWidth()) / 2;
        int y = (screenSize.height - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
        dialog.setDefaultCloseOperation(dialog.HIDE_ON_CLOSE);


        //            SelectFileUtil.selectDir(curGitPath, selectedFile -> {
//                createGitHelper(selectedFile.getAbsolutePath());
//            });


        Container pane= dialog.getContentPane();
        pane.setLayout(null);
        JLabel label1 = new JLabel("密码: ");
        label1.setBounds(PADDING, PADDING, 40, 30);
        pane.add(label1);
        JPasswordField  un = new JPasswordField();
        un.setBounds(60, PADDING, 80, 30);
        pane.add(un);
        dialog.setVisible(true);
    }

    private interface OnClickListener {
        void onClick(MouseEvent event);
    }

    private void addRightButton(Box box, String text, OnClickListener listener) {
        box.add(Box.createVerticalStrut(10));
        JButton button = new JButton(text);
        //居中
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                listener.onClick(e);
            }
        });
        box.add(button);
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
