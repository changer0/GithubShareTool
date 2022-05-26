package org.lulu.share;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * 文件列表
 */
public class FileList extends JList<FileList.FileItem> {
    private static final FileSystemView fsv = FileSystemView.getFileSystemView();
    private File nowDir;
    private FileOpenListener fileOpenListener;

    public FileList() {
        setCellRenderer(new FileListCellRenderer());
        setModel(new FileListModel());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if (e.getClickCount() == 2) {
                    int index = locationToIndex(e.getPoint());
                    if (index >= 0) {
                        FileItem v = getModel().getElementAt(index);
                        if (v.file.isDirectory())
                            openItem(v);
                    }
                }
            }
        });
    }

    public void openItem(File file) {
        if (file.isFile()) {
            file = fsv.getParentDirectory(file);
        }
        openItem(new FileItem(fsv.getSystemDisplayName(file), fsv.getSystemIcon(file), file));
    }

    public void openItem(FileItem item) {
        File[] subFiles;
        if (item.file != null)
            subFiles = fsv.getFiles(item.file, false);
        else
            subFiles = fsv.getRoots();

        ((DefaultListModel<FileItem>) getModel()).clear();
        for (File subFile : subFiles)
            ((DefaultListModel<FileItem>) getModel()).addElement(new FileItem(fsv.getSystemDisplayName(subFile),
                    fsv.getSystemIcon(subFile), subFile));

        if (item.file != null)
            nowDir = item.file;

        // 回调监听器
        if (fileOpenListener != null)
            fileOpenListener.fileOpenEvent(nowDir);
    }

    public void returnDir() {
        if (nowDir == null)
            return;

        nowDir = fsv.getParentDirectory(nowDir);
        openItem(new FileItem(fsv.getSystemDisplayName(nowDir), fsv.getSystemIcon(nowDir), nowDir));
    }

    public FileOpenListener getFileOpenListener() {
        return fileOpenListener;
    }

    public void setFileOpenListener(FileOpenListener fileOpenListener) {
        this.fileOpenListener = fileOpenListener;
    }

    // 文件打开监听器
    public interface FileOpenListener {
        void fileOpenEvent(File nowDir);
    }

    private static class FileListModel extends DefaultListModel<FileItem> {
        public FileListModel() {
            // 初始化系统根目录
            File[] roots = fsv.getRoots();
            for (File root : roots)
                addElement(new FileItem(fsv.getSystemDisplayName(root), fsv.getSystemIcon(root), root));
        }
    }

    private static class FileListCellRenderer extends JLabel implements ListCellRenderer<FileItem> {
        public FileListCellRenderer() {
            setPreferredSize(new Dimension(getWidth(), 25));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FileItem> list, FileItem value, int index,
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
    public static class FileItem {
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
