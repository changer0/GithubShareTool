package org.lulu.share;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

public class GitShareFrame extends JFrame {

    public static final int PADDING = 10;

    public static final String KEY_UN = "un";

    public static final String KEY_PWD = "pwd";

    public static final int W = 500;

    public static final int H = 500;

    private final LogPanel log = new LogPanel();

    private final FileList fileList = new FileList();

    private final JComboBox<String> branchListBox = new JComboBox<>();

    /**
     * 当前 Git 路径
     */
    private String curGitPath;

    private GitHelper gitHelper;

    private CredentialsProvider credentialsProvider;
    private JLabel gitPathLabel;

    private JCheckBoxSet checkBoxSet = new JCheckBoxSet();
    private volatile boolean isReleasing = false;

    public GitShareFrame() throws HeadlessException {
        super("GitHub 分享工具");
        fileList.setShowHide(false);
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

    private void configCredentialProvider() {
        String un = KVStorage.get(KEY_UN, "");
        String pwd = KVStorage.get(KEY_PWD, "");
        if (StringUtils.isAnyEmpty(un, pwd)) {
            log.e("未配置 GitHub 令牌 =>【令牌配置】");
            return;
        }
        log.i("用户名密码配置成功");
        credentialsProvider = new UsernamePasswordCredentialsProvider(un, pwd);

        if (gitHelper != null) {
            gitHelper.setProvider(credentialsProvider);
        }
    }

    private void createGitHelper(String gitPath) {
        if (StringUtils.isEmpty(gitPath)) {
            log.e("未配置仓库 =>拖拽或【仓库配置】");
            return;
        }
        try {
            gitHelper = new GitHelper(new File(gitPath));
            configCredentialProvider();
            curGitPath = gitPath;
            KVStorage.put("git_path", curGitPath);
            log.i("仓库地址已配置: " + gitPath);
        } catch (IOException e) {
            log.e("请确保该目录为 Git 工程!");
            gitHelper = null;
            curGitPath = "";
        }

        refreshCurGitPath();
        refreshBranchList();
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
            log.e("暂未发布 => 【一键发布】");
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
            try {
                relativePath.append("/").append(URLEncoder.encode(s, "UTF-8").replace("+", "%20"));
            } catch (UnsupportedEncodingException e) {
                log.e("编码失败: " + e.getMessage());
            }
        }
        String curBranch = getCurBranch();
        String result;
        if (file.getName().endsWith("html")) {
            String htmlPreviewPrefix = "https://htmlpreview.github.io/?";
            result = htmlPreviewPrefix + remoteUrl + "/blob/" + curBranch + relativePath;
        } else {
            result = "https://raw.githubusercontent.com" + remoteUrl.replace("https://github.com", "") + "/" + curBranch + relativePath;
        }
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
        if (gitHelper == null) {
            createGitHelper(curGitPath);
            return;
        }
        isReleasing = true;
        TaskHandler.getInstance().enqueue(() -> {
            try {
                log.i("切换分支: " + getCurBranch());
                gitHelper.createBranchWithRemote(getCurBranch());
                log.i("开始发布");
                log.i("开始拉取...");
                gitHelper.pull();
                log.i("开始提交...");
                gitHelper.commit("commit: " + System.currentTimeMillis());
                log.i("开始推送...");
                gitHelper.push();
                log.i("发布完成");
            } catch (Exception e) {
                log.e("git 发生错误: 请检查配置" + e.getMessage());
            }
            isReleasing = false;
        });

    }

    private boolean checkNeedRelease() {
        Repository repository = gitHelper.getRepository();
        String curBranch = getCurBranch();
        try {
            // 1. 检查是否提交
            if (!repository.resolve("origin/" + curBranch).equals(repository.resolve(curBranch))) {
                return true;
            }
            //2. 检查是否有未 add commit 的
            if (!gitHelper.status().isClean()) {
                return true;
            }
        } catch (IOException | GitAPIException e) {
            log.e("git 发生错误: " + e.getMessage());
        }
        return false;
    }

    public String getCurBranch() {
        return KVStorage.get("cur_branch", "master");
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

        addRemoteBranchList();
    }

    private void refreshBranchList() {
        try {
            if (gitHelper == null) {
                branchListBox.setVisible(false);
                return;
            }
            String[] remoteList = gitHelper.getRemoteBranch().toArray(new String[0]);
            if (remoteList.length <= 0) {
                branchListBox.setVisible(false);
                return;
            }
            branchListBox.setVisible(true);
            Arrays.sort(remoteList);
            DefaultComboBoxModel<String> modelList = new DefaultComboBoxModel<>(remoteList);
            branchListBox.setModel(modelList);
            branchListBox.setSelectedItem(getCurBranch());
            branchListBox.addActionListener(e -> {
                String selectedItem = (String) modelList.getSelectedItem();
                log.i("选中" + selectedItem + "分支");
                KVStorage.put("cur_branch", selectedItem);
            });

        } catch (GitAPIException e) {
            log.e("获取远程分支失败!");
        }

    }

    private void addRemoteBranchList() {
        JLabel comp = new JLabel("分支:");
        comp.setBounds(W - 160, PADDING, 80, 20);
        add(comp);

        branchListBox.setBounds(W - 120, PADDING, 80, 20);
        add(branchListBox);
        String curBranch = getCurBranch();
        //提前保存一份
        KVStorage.put("cur_branch", curBranch);
        branchListBox.setVisible(false);
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
            JMenuItem copyLink = new JMenuItem("复制分享链接");
            JMenuItem delete = new JMenuItem("删除");
            copyLink.addActionListener(e1 -> copyShareLink(file));
            delete.addActionListener(e12 -> {
                try {
                    File dir = file.getParentFile();
                    FileUtils.deleteQuietly(file);
                    fileList.openItem(dir);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            jPopupMenu.add(copyLink);
            jPopupMenu.add(delete);
            jPopupMenu.show(fileList, e.getX(), e.getY());
        });

        jPanel.setBounds(PADDING, 40, W - 130, H - 200);
        jPanel.setLayout(new BorderLayout(2, 5));
        jPanel.setBackground(Color.WHITE);
        JScrollPane comp = new JScrollPane(fileList);

        jPanel.add(jTextField, BorderLayout.NORTH);
        jPanel.add(comp);
        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(0, 25));
        JTextFieldHintListener hintListener = new JTextFieldHintListener(searchField, "在此输入开始搜索文件");
        searchField.addFocusListener(hintListener);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                trySearch(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                trySearch(e);

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        jPanel.add(searchField, BorderLayout.SOUTH);

        add(jPanel);
    }

    private void trySearch(DocumentEvent e) {
        Document document = e.getDocument();
        try {
            String searchKey = document.getText(0, document.getLength());
            //log.i("SearchKey: " + searchKey);
            if (StringUtils.isEmpty(searchKey) || StringUtils.equals(searchKey, "在此输入开始搜索文件")) {
                fileList.refresh();
            } else {
                fileList.searchFile(searchKey);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
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

        addRightButton(box, "仓库地址", event -> {
            SelectFileUtil.selectDir(curGitPath, selectedFile -> {
                createGitHelper(selectedFile.getAbsolutePath());
            });
        });
        addRightButton(box, "令牌配置", event -> {
            showGitTokenConfigDialog();
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
        addShowHintFileCheck(box);
        jPanel.add(box);
        add(jPanel);
    }

    private void addShowHintFileCheck(Box box) {
        box.add(Box.createVerticalStrut(10));
        boolean showHintFile = StringUtils.equals(KVStorage.get("show_hint_file", "FALSE"), "TRUE");
        fileList.setShowHide(showHintFile);
        checkBoxSet.showHintFile.setSelected(showHintFile);
        checkBoxSet.showHintFile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                KVStorage.put("show_hint_file", checkBoxSet.showHintFile.isSelected() ? "TRUE" : "FALSE");
                fileList.setShowHide(checkBoxSet.showHintFile.isSelected());
                fileList.refresh();
            }
        });
        box.add(checkBoxSet.showHintFile);
    }

    public void showGitTokenConfigDialog() {
        JDialog dialog = new JDialog(this, "令牌配置", true);
        dialog.setSize(202, 180);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - dialog.getWidth()) / 2;
        int y = (screenSize.height - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
        dialog.setDefaultCloseOperation(dialog.HIDE_ON_CLOSE);

        Container pane = dialog.getContentPane();
        pane.setLayout(null);

        JLabel label0 = new JLabel("GitHub 访问令牌: ");
        label0.setBounds(PADDING, PADDING, 100, 25);
        pane.add(label0);
        JLabel label1 = new JLabel("用户名:");
        label1.setBounds(PADDING, PADDING + 30, 50, 25);
        pane.add(label1);
        JTextField unField = new JTextField();
        unField.setBounds(60, PADDING + 30, 80, 25);
        pane.add(unField);

        JLabel label2 = new JLabel("密码:");
        label2.setBounds(PADDING, PADDING + 60, 50, 25);
        pane.add(label2);
        JPasswordField pwdField = new JPasswordField();
        pwdField.setBounds(60, PADDING + 60, 80, 25);
        pane.add(pwdField);

        String un = KVStorage.get(KEY_UN, "");
        String pwd = KVStorage.get(KEY_PWD, "");
        unField.setText(un);
        pwdField.setText(pwd);

        JButton gitPathButton = new JButton("保存");
        gitPathButton.setBounds(PADDING + 10, PADDING + 90, 60, 30);
        gitPathButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                KVStorage.put(KEY_UN, unField.getText());
                KVStorage.put(KEY_PWD, pwdField.getText());
                //log.i("用户名密码已保存");
                dialog.dispose();
                configCredentialProvider();
            }
        });
        pane.add(gitPathButton);

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
