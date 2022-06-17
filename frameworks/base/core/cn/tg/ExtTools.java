package cn.tg;

import android.app.ITGRom;
import android.app.ActivityThread;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

// add
public class ExtTools {
    public static List<PackageItem> mikConfigs;


    public static void loadGadget() {
        String processName = ActivityThread.currentProcessName();
        Log.e("tgrom", "loadGadget enter package:" + processName);
        for (PackageItem item : mikConfigs) {
            Log.e("tgrom", "loadGadget package:" + processName + " mikconfig:" + item.packageName);
            if (item.packageName.equals(processName)) {
                try {
                    if (item.fridaJsPath.length() <= 0) {
                        Log.e("tgrom", "loadGadget package:" + processName + " jspath <=0");
                        continue;
                    }
                    String gadPath = "";
                    String arch = System.getProperty("os.arch");
                    if (item.gadgetPath != null && item.gadgetPath.length() > 0) {
                        if (arch.indexOf("64") >= 0) {
                            gadPath = item.gadgetArm64Path;
                        } else {
                            gadPath = item.gadgetPath;
                        }
                    } else {
                        boolean use14 = false;
                        ITGRom mikrom = getiMikRom();
                        if (mikrom != null) {
                            String res = mikrom.readFile("/data/system/fver14.conf");
                            Log.e("tgrom", "fver14.conf data " + res);
                            if (res.contains("1")) {
                                use14 = true;
                            }
                        }
                        if (System.getProperty("os.arch").indexOf("64") >= 0) {
                            if (use14) {
                                gadPath = "/system/lib64/libfdgg14.so";
                            } else {
                                gadPath = "/system/lib64/libfdgg15.so";
                            }
                        } else {
                            if (use14) {
                                gadPath = "/system/lib/libfdgg14.so";
                            } else {
                                gadPath = "/system/lib/libfdgg15.so";
                            }
                        }
                    }
                    Log.e("mikrom", "loadGadget package:" + processName + " gadPath:" + gadPath);
                    File gadfile = new File(gadPath);
                    String name = gadfile.getName().replace(".so", "");
                    String configPath = "/data/data/" + processName + "/" + name + ".config.so";
                    if (item.fridaJsPath.equals("listen") || item.fridaJsPath.equals("listen_wait")) {
                        WriteConfig(configPath, item.fridaJsPath, item.port);
                    } else {
                        File file = new File(item.fridaJsPath);
                        if (!file.exists()) {
                            file = new File("/data/data/" + processName + "/" + file.getName());
                        }
                        if (!file.exists()) {
                            Log.e("mikrom", "loadGadget package:" + processName + " frida js path:" + item.fridaJsPath + " not found");
                            continue;
                        }
                        WriteConfig(configPath, item.fridaJsPath, item.port);
                    }
                    loadSo(gadPath);
                    Log.e("mikrom", "loadGadget package:" + processName + " over");
                } catch (Exception ex) {
                    Log.e("mikrom", "loadGadget package:" + processName + " frida js path:" + item.fridaJsPath + " load err:" + ex.getMessage());
                }
                break;
            }
        }
    }

    private static ITGRom iTGRom = null;

    public static ITGRom getiMikRom() {
        if (iTGRom == null) {
            try {
                IBinder binder = ServiceManager.getService("tgrom");
                if (binder == null) {
                    Log.d("tgrom", "getiMikRom binder is null");
                    return iTGRom;
                }
                iTGRom = ITGRom.Stub.asInterface(binder);
            } catch (Exception e) {
                Log.d("tgrom", "getiMikRom exception " + e.getMessage());
                e.printStackTrace();
            }
        }
        return iTGRom;
    }

    public static void WriteConfig(String path, String jspath, int port) {
        try {
            FileWriter writer = new FileWriter(path);
            String fconfig = "";
            if (jspath.equals("listen")) {
                fconfig = "{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"listen\",\n" +
                        "    \"address\": \"0.0.0.0\",\n" +
                        "    \"port\": " + port + ",\n" +
                        "    \"on_load\": \"resume\"\n" +
                        "  }\n" +
                        "}";
            } else if (jspath.equals("listen_wait")) {
                fconfig = "{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"listen\",\n" +
                        "    \"address\": \"0.0.0.0\",\n" +
                        "    \"port\": " + port + ",\n" +
                        "    \"on_load\": \"wait\"\n" +
                        "  }\n" +
                        "}";
            } else {
                String processName = ActivityThread.currentProcessName();
                String fName = jspath.trim();
                String fileName = fName.substring(fName.lastIndexOf("/") + 1);
                String newJsPath = "/data/data/" + processName + "/" + fileName;
                mycopy(jspath, newJsPath);
                int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
                FileUtils.setPermissions(newJsPath, perm, -1, -1);//将权限改为777
                fconfig = "{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"script\",\n" +
                        "    \"path\": \"" + newJsPath + "\"\n" +
                        "  }\n" +
                        "}";
            }
            Log.e("mikrom", "WriteConfig config:" + fconfig);
            writer.write(fconfig);
            writer.close();
        } catch (IOException e) {
            Log.e("mikrom", "WriteConfig err:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadSo(String path) {
        String processName = ActivityThread.currentProcessName();
        String fName = path.trim();
        String fileName = fName.substring(fName.lastIndexOf("/") + 1);
        String tagPath = "/data/data/" + processName + "/" + fileName;//64位so的目录
        mycopy(path, tagPath);
        int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
        FileUtils.setPermissions(tagPath, perm, -1, -1);//将权限改为777
        File file = new File(tagPath);
        if (file.exists()) {
            Log.e("mikrom", "load so src:" + path + " to:" + tagPath);
            System.load(tagPath);
            file.delete();//用完就删否则不会更新
        }
    }

    public static void mycopy(String srcFileName, String trcFileName) {
        InputStream in = null;
        OutputStream out = null;
        try {
            // in = File.open(srcFileName);
            in = new FileInputStream(srcFileName);
            out = new FileOutputStream(trcFileName);
            byte[] bytes = new byte[1024];
            int i;
            while ((i = in.read(bytes)) != -1)
                out.write(bytes, 0, i);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
