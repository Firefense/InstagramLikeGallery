package com.firefishcomms.library.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageGalleryService {

    private final Folder DEFAULT_FOLDER_DIRECTORY = new Folder("All photos", "all");

    private static ImageGalleryService instance;
    private static Context context;

    private List<File> imageFiles;
    private final String[] okFileExtensions =  new String[] {"jpg", "png", "gif","jpeg"};

    private ImageGalleryService(){
        imageFiles = new ArrayList<>();
    }

    public static ImageGalleryService getInstance(Context c){
        if(instance == null){
            instance = new ImageGalleryService();
        }

        context = c;
        return instance;
    }

    public List<Folder> getAllImageFolderDirectories(){
        List<Folder> imageFolders = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return imageFolders;
        }
        imageFolders.add(DEFAULT_FOLDER_DIRECTORY);

        addFolderIfHasImage(imageFolders, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
        addFolderIfHasImage(imageFolders, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        addFolderIfHasImage(imageFolders, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
        addFolderIfHasImage(imageFolders, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));

        return imageFolders;
    }

    private void addFolderIfHasImage(List<Folder> imageFolders, File currentDirectory){
        if(currentDirectory.getName().startsWith(".")) return;
        boolean isCurrentDirectoryHasImage = false;
        if(currentDirectory.isDirectory()){
            for(File file : currentDirectory.listFiles()){
                if(file.isFile()){
                    if(isCurrentDirectoryHasImage) continue;
                    else if(isImageFile(file)){
                        isCurrentDirectoryHasImage = true;
                        imageFolders.add(new Folder(currentDirectory.getName(), currentDirectory.toString()));
                    }
                } else if(file.isDirectory()){
                    addFolderIfHasImage(imageFolders, file);
                }
            }
        }
    }

    /**
     * To retrieve all images found in the desired path
     * @param folder only Folder.path will be used in this method
     * @return list of images found
     */
    public List<File> retrieveImages(Folder folder){

        if(folder == null || folder.path == null) {
            imageFiles.clear();
            return imageFiles;
        } else if(folder.equals(DEFAULT_FOLDER_DIRECTORY)){
            return retrieveAllImages();
        } else {
            return retrieveImages(folder.path);
        }
    }

    /**
     * To retrieve all images found in the desired path
     * @param path
     * @return list of images found
     */
    public List<File> retrieveImages(String path){
        imageFiles.clear();

        File files = new File(path);
        if(files.isDirectory()){
            if(!files.getName().startsWith("."))
                getImages(files);
        }

        Collections.sort(imageFiles, new Comparator<File>() {

            @Override
            public int compare(File file1, File file2) {
                long k = file1.lastModified() - file2.lastModified();
                if(k > 0){
                    return -1;
                }else if(k == 0){
                    return 0;
                }else{
                    return 1;
                }
            }
        });

        return imageFiles;
    }

    private List<File> retrieveAllImages(){
        imageFiles.clear();

        getImages(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
        getImages(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        getImages(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
        getImages(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));

        Collections.sort(imageFiles, new Comparator<File>() {

            @Override
            public int compare(File file1, File file2) {
                long k = file1.lastModified() - file2.lastModified();
                if(k > 0){
                    return -1;
                }else if(k == 0){
                    return 0;
                }else{
                    return 1;
                }
            }
        });

        return imageFiles;
    }

    private void getImages(File files){
        // listFiles() may return null
        if(files.listFiles() == null) return;

        for(File file : files.listFiles()){
            if(file.isDirectory()){
                if(!file.getName().startsWith("."))
                    getImages(file);
            } else if(isImageFile(file)){
                imageFiles.add(file);
            }
        }
    }

    private boolean isImageFile(File file) {
        for (String extension : okFileExtensions) {
            if (file.getName().toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public class Folder{
        public String name;
        public String path;

        public Folder(String n, String p){
            this.name = n;
            this.path = p;
        }

        @Override
        public boolean equals(Object object){
            if(object instanceof Folder){
                Folder compared = (Folder) object;
                if(this.name != null && this.name.equals(compared.name) && this.path != null && this.path.equals(compared.path))
                    return true;
            }
            return false;
        }
    }
}
