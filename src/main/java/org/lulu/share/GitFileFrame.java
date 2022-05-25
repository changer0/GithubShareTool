package org.lulu.share;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class GitFileFrame {


    private static final FileSystemView fsv = FileSystemView.getFileSystemView();

    private final FileList fileList = new FileList();

    public FileList getFileList() {
        return fileList;
    }

    public GitFileFrame() {
        //fsv.getSystemDisplayName(new File("C:\\Users\\zll88\\IdeaProjects\\GithubShareUtil"));
        File nowDir = new File("C:\\Users\\zll88\\IdeaProjects\\GithubShareUtil");
        fileList.openItem(new GitFileFrame.FileItem(fsv.getSystemDisplayName(nowDir), fsv.getSystemIcon(nowDir), nowDir));
    }

    // 文件打开监听器
    private interface FileOpenListener {
        void fileOpenEvent(File nowDir);
    }

    /**
     * 文件列表
     */
    private static class FileList extends JList<GitFileFrame.FileItem> {
        private File nowDir;
        private GitFileFrame.FileOpenListener fileOpenListener;

        public FileList() {
            setCellRenderer(new GitFileFrame.FileListCellRenderer());
            setModel(new GitFileFrame.FileListModel());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);

                    if (e.getClickCount() == 2) {
                        int index = locationToIndex(e.getPoint());
                        if (index >= 0) {
                            GitFileFrame.FileItem v = getModel().getElementAt(index);
                            if (v.file.isDirectory())
                                openItem(v);
                        }
                    }
                }
            });
        }

        private void openItem(GitFileFrame.FileItem item) {
            File[] subFiles;
            if (item.file != null)
                subFiles = fsv.getFiles(item.file, false);
            else
                subFiles = fsv.getRoots();

            ((DefaultListModel<GitFileFrame.FileItem>) getModel()).clear();
            for (File subFile : subFiles)
                ((DefaultListModel<GitFileFrame.FileItem>) getModel()).addElement(new GitFileFrame.FileItem(fsv.getSystemDisplayName(subFile),
                        fsv.getSystemIcon(subFile), subFile));

            if (item.file != null)
                nowDir = item.file;

            // 回调监听器
            if (fileOpenListener != null)
                fileOpenListener.fileOpenEvent(nowDir);
        }

        private void returnDir() {
            if (nowDir == null)
                return;

            nowDir = fsv.getParentDirectory(nowDir);
            openItem(new GitFileFrame.FileItem(fsv.getSystemDisplayName(nowDir), fsv.getSystemIcon(nowDir), nowDir));
        }

        public GitFileFrame.FileOpenListener getFileOpenListener() {
            return fileOpenListener;
        }

        public void setFileOpenListener(GitFileFrame.FileOpenListener fileOpenListener) {
            this.fileOpenListener = fileOpenListener;
        }
    }

    private static class FileListModel extends DefaultListModel<GitFileFrame.FileItem> {
        public FileListModel() {
            // 初始化系统根目录
            File[] roots = fsv.getRoots();
            for (File root : roots)
                addElement(new GitFileFrame.FileItem(fsv.getSystemDisplayName(root), fsv.getSystemIcon(root), root));
        }
    }

    private static class FileListCellRenderer extends JLabel implements ListCellRenderer<GitFileFrame.FileItem> {
        public FileListCellRenderer() {
            setPreferredSize(new Dimension(getWidth(), 25));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GitFileFrame.FileItem> list, GitFileFrame.FileItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(value.name);
            setIcon(value.icon);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

    // 文件项
    private static class FileItem {
        public String name;// 文件名
        public Icon icon;// 文件图标
        public File file;// 文件

        public FileItem(String name, Icon icon, File file) {
            this.name = name;
            this.icon = icon;
            this.file = file;
        }
    }
}
