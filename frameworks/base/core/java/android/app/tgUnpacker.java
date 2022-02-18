package android.app;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.lang.reflect.*;

import android.app.ActivityThread;
import android.app.Application;
import android.app.IMikRom;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dalvik.system.DexClassLoader;

class tgUnpacker {
    //为了反射封装，根据类名和字段名，反射获取字段
    public static Field getClassField(ClassLoader classloader, String class_name,
                                      String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object getClassFieldObject(ClassLoader classloader, String class_name, Object obj,
                                             String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            Object result = null;
            result = field.get(obj);
            return result;
            //field.setAccessible(true);
            //return field;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object invokeStaticMethod(String class_name,
                                            String method_name, Class[] pareTyple, Object[] pareVaules) {

        try {
            Class obj_class = Class.forName(class_name);
            Method method = obj_class.getMethod(method_name, pareTyple);
            return method.invoke(null, pareVaules);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object getFieldObject(String class_name, Object obj,
                                        String filedName) {
        try {
            Class obj_class = Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Application getCurrentApplication(){
        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Application mInitialApplication = (Application) getFieldObject("android.app.ActivityThread",
                currentActivityThread, "mInitialApplication");
        Object loadedApkInfo = getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplication");
        return mApplication;
    }

    public static ClassLoader getClassloader() {
        ClassLoader resultClassloader = null;
        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Application mInitialApplication = (Application) getFieldObject("android.app.ActivityThread",
                currentActivityThread, "mInitialApplication");
        Object loadedApkInfo = getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplication");
        Log.e("tgrom", "go into app->" + "packagename:" + mApplication.getPackageName());
        resultClassloader = mApplication.getClassLoader();
        return resultClassloader;
    }

    public static boolean isBreakClass(String clsname){
        boolean isbreak = false;
        for(String item :bClass){
            if(item.trim().length()>0){
                if(clsname.contains(item)){
                    isbreak = true;
                    break;
                }
            }
        }
        return isbreak;
    }

    public static boolean isWhiteClass(String clsname){
        boolean iswhite = false;
        for(String item :whiteClass){
            if(clsname.contains(item)){
                iswhite = true;
                break;
            }
        }
        return iswhite;
    }

    //取指定类的所有构造函数，和所有函数，使用dumpMethodCode函数来把这些函数给保存出来
    public static void loadClassAndInvoke(ClassLoader appClassloader, String eachclassname, Method dumpMethodCode_method) {
        if(whiteClass.size()>0){
            if(!isWhiteClass(eachclassname)){
                Log.e("tgrom", "loadClassAndInvoke->" + "classname:" + eachclassname+" is not white Class");
                return;
            }
            if(bClass.size()>0){
                if(isBreakClass(eachclassname)){
                    Log.e("tgrom", "loadClassAndInvoke->" + "classname:" + eachclassname+" is break Class");
                    return;
                }
            }
        }else{
            if(bClass.size()>0){
                if(isBreakClass(eachclassname)){
                    Log.e("tgrom", "loadClassAndInvoke->" + "classname:" + eachclassname+" is break Class");
                    return;
                }
            }
        }

        Class resultclass = null;
        Log.e("tgrom", "go into loadClassAndInvoke->" + "classname:" + eachclassname);
        try {
            resultclass = appClassloader.loadClass(eachclassname);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } catch (Error e) {
            e.printStackTrace();
            return;
        }
        if (resultclass != null) {
            try {
                Constructor<?> cons[] = resultclass.getDeclaredConstructors();
                for (Constructor<?> constructor : cons) {
                    if (dumpMethodCode_method != null) {
                        try {
                            if(constructor.getName().contains("cn.mik.")){
                                continue;
                            }
                            Log.e("tgrom", "classname:" + eachclassname+ " constructor->invoke "+constructor.getName());
                            dumpMethodCode_method.invoke(null, constructor);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        } catch (Error e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        Log.e("tgrom", "dumpMethodCode_method is null ");
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
            try {
                Method[] methods = resultclass.getDeclaredMethods();
                if (methods != null) {
                    Log.e("tgrom", "classname:" + eachclassname+ " start invoke");
                    for (Method m : methods) {
                        if (dumpMethodCode_method != null) {
                            try {
                                if(m.getName().contains("cn.mik.")){
                                    continue;
                                }
                                Log.e("tgrom", "classname:" + eachclassname+ " method->invoke:" + m.getName());
                                dumpMethodCode_method.invoke(null, m);
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            } catch (Error e) {
                                e.printStackTrace();
                                continue;
                            }
                        } else {
                            Log.e("tgrom", "dumpMethodCode_method is null ");
                        }
                    }
                    Log.e("tgrom", "go into loadClassAndInvoke->"   + "classname:" + eachclassname+ " end invoke");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
        }
    }

    //根据classLoader->pathList->dexElements拿到dexFile
    //然后拿到mCookie后，使用getClassNameList获取到所有类名。
    //loadClassAndInvoke处理所有类名导出所有函数
    //dumpMethodCode这个函数是fart自己加在DexFile中的
    public static void tgWithClassLoader(ClassLoader appClassloader) {
        Log.e("tgrom", "tgWithClassLoader "+appClassloader.toString());
        List<Object> dexFilesArray = new ArrayList<Object>();
        Field paist_Field = (Field) getClassField(appClassloader, "dalvik.system.BaseDexClassLoader", "pathList");
        Object pathList_object = getFieldObject("dalvik.system.BaseDexClassLoader", appClassloader, "pathList");
        Object[] ElementsArray = (Object[]) getFieldObject("dalvik.system.DexPathList", pathList_object, "dexElements");
        Field dexFile_fileField = null;
        try {
            dexFile_fileField = (Field) getClassField(appClassloader, "dalvik.system.DexPathList$Element", "dexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Method getClassNameList_method = null;
        Method defineClass_method = null;
        Method dumpDexFile_method = null;
        Method dumpMethodCode_method = null;
        Method dumpRepair_method = null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("getClassNameList")) {
                getClassNameList_method = field;
                getClassNameList_method.setAccessible(true);
            }
            if (field.getName().equals("defineClassNative")) {
                defineClass_method = field;
                defineClass_method.setAccessible(true);
            }
            if (field.getName().equals("dumpDexFile")) {
                dumpDexFile_method = field;
                dumpDexFile_method.setAccessible(true);
            }
            if (field.getName().equals("tgextMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
            if (field.getName().equals("dumpRepair")) {
                dumpRepair_method = field;
                dumpRepair_method.setAccessible(true);
            }
        }
        Field mCookiefield = getClassField(appClassloader, "dalvik.system.DexFile", "mCookie");
        Log.e("tgrom", "->methods dalvik.system.DexPathList.ElementsArray.length:" + ElementsArray.length);
        for (int j = 0; j < ElementsArray.length; j++) {
            Object element = ElementsArray[j];
            Object dexfile = null;
            try {
                dexfile = (Object) dexFile_fileField.get(element);
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
            if (dexfile == null) {
                Log.e("tgrom", "dexfile is null");
                continue;
            }
            if (dexfile != null) {
                dexFilesArray.add(dexfile);
                Object mcookie = getClassFieldObject(appClassloader, "dalvik.system.DexFile", dexfile, "mCookie");
                if (mcookie == null) {
                    Object mInternalCookie = getClassFieldObject(appClassloader, "dalvik.system.DexFile", dexfile, "mInternalCookie");
                    if(mInternalCookie!=null)
                    {
                        mcookie=mInternalCookie;
                    }else{
                        Log.e("tgrom", "->err get mInternalCookie is null");
                        continue;
                    }

                }
                String[] classnames = null;
                try {
                    classnames = (String[]) getClassNameList_method.invoke(dexfile, mcookie);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                } catch (Error e) {
                    e.printStackTrace();
                    continue;
                }
                if (classnames != null) {
                    Log.e("tgrom", "all classes "+String.join(",",classnames));
                    for (String eachclassname : classnames) {
                        loadClassAndInvoke(appClassloader, eachclassname, dumpMethodCode_method);
                    }
                    if(dumpRepair_method!=null){
                        Log.e("tgrom", "tgWithClassLoader dumpRepair");
                        try {
                            dumpRepair_method.invoke(null);
                        }catch(Exception ex){
                            Log.e("tgrom", "tgWithClassList dumpRepair invoke err:"+ex.getMessage());
                        }
                    }else{
                        Log.e("tgrom", "tgWithClassLoader dumpRepair is null");
                    }
                }

            }
        }
        return;
    }

    public static void tgUnpacker() {
        Log.e("tgrom", "tgUnpacker start");
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            Log.e("tgrom", "appClassloader is null");
            return;
        }
        ClassLoader tmpClassloader=appClassloader;
        ClassLoader parentClassloader=appClassloader.getParent();
        if(appClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
        {
            tgWithClassLoader(appClassloader);
        }
        while(parentClassloader!=null){
            if(parentClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
            {
                tgWithClassLoader(parentClassloader);
            }
            tmpClassloader=parentClassloader;
            parentClassloader=parentClassloader.getParent();
        }
    }

    public static void SetRomConfig(PackageItem item){
        Log.e("tgrom", "SetRomConfig start");
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            Log.e("tgrom", "SetRomConfig appClassloader is null");
            return;
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            Log.e("tgrom", "SetRomConfig loadClass err:"+e.getMessage());
            e.printStackTrace();
        }
        Method settgRomConfig_method = null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("settgRomConfig")) {
                settgRomConfig_method = field;
                settgRomConfig_method.setAccessible(true);
            }
        }
        if(settgRomConfig_method==null){
            Log.e("tgrom", "SetRomConfig settgRomConfig_method is null");
            return;
        }
        try{
            Log.e("tgrom", "SetRomConfig invoke");
            settgRomConfig_method.invoke(null,item);
        }catch (Exception e) {
            Log.e("tgrom", "SetRomConfig settgRomConfig_method.invoke "+e.getMessage());
            e.printStackTrace();
        }
    }
    public static List<PackageItem> mikConfigs;
    public static List<String> bClass=new ArrayList<String>();
    public static List<String> whiteClass=new ArrayList<String>();
    public static String whitePath="";

    public static String readConfig(String path){
        String res="";
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            char[] buf=new char[8000];
            int num=br.read(buf);
            if(num<=0){
                Log.e("tgrom", String.format("initConfig err:%s is null",path));
                return "";
            }
            br.close();
            res=new String(buf,0,num);
        }catch(Exception ex){
            Log.e("tgrom", "initConfig err:" + ex.getMessage());
            return "";
        }
        return res;
    }
    private static ItgRom itgRom=null;
    public static ItgRom getitgRom() {
        if (itgRom == null) {
            try {
                Class localClass = Class.forName("android.os.ServiceManager");
                Method getService = localClass.getMethod("getService", new Class[] {String.class});
                if(getService != null) {
                    Object objResult = getService.invoke(localClass, new Object[]{"tgrom"});
                    if (objResult != null) {
                        IBinder binder = (IBinder) objResult;
                        itgRom = ItgRom.Stub.asInterface(binder);
                    }
                }
            } catch (Exception e) {
                Log.d("MikManager",e.getMessage());
                e.printStackTrace();
            }
        }
        return itgRom;
    }

    public static String getMikConfig(){
        try {
            ItgRom tgrom=getitgRom();
            if(tgrom==null){
                return "";
            }
            return tgrom.readFile("/data/system/mik.conf");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void initConfig(){
        try {
            mikConfigs=new ArrayList<PackageItem>();
            String tgromConfigJson=getMikConfig();
            if(tgromConfigJson.length()>5){
                final JSONArray arr = new JSONArray(tgromConfigJson);
                Log.e("tgrom", "initConfig package count:"+arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject jobj = arr.getJSONObject(i);
                    PackageItem cfg=new PackageItem();
                    cfg.enabled=jobj.getBoolean("enabled");
                    cfg.packageName = jobj.getString("packageName");
                    cfg.appName = jobj.getString("appName");
                    if(!cfg.enabled){
                        Log.e("tgrom", "initConfig enabled is false skip "+cfg.packageName);
                        continue;
                    }
                    cfg.whiteClass=jobj.getString("whiteClass");
                    cfg.whitePath=jobj.getString("whitePath");
                    cfg.breakClass = jobj.getString("breakClass");
                    cfg.isTuoke = jobj.getBoolean("isTuoke");
                    cfg.isDeep = jobj.getBoolean("isDeep");

                    cfg.isInvokePrint = jobj.getBoolean("isInvokePrint");
                    cfg.isJNIMethodPrint = jobj.getBoolean("isJNIMethodPrint");
                    cfg.isRegisterNativePrint = jobj.getBoolean("isRegisterNativePrint");

                    cfg.traceMethod = jobj.getString("traceMethod");
                    cfg.sleepNativeMethod=jobj.getString("sleepNativeMethod");
                    cfg.fridaJsPath=jobj.getString("fridaJsPath");
                    cfg.port=jobj.getInt("port");
                    cfg.gadgetPath=jobj.getString("gadgetPath");
                    cfg.gadgetArm64Path=jobj.getString("gadgetArm64Path");
                    cfg.soPath=jobj.getString("soPath");
                    cfg.dexPath=jobj.getString("dexPath");
                    cfg.isDobby=jobj.getBoolean("isDobby");
                    mikConfigs.add(cfg);
                    Log.e("tgrom", "initConfig packageName" + cfg.packageName);
                }
            }
            String breakPath="/data/system/break.conf";
            String breakData= getitgRom().readFile(breakPath);
            for(String item : breakData.split("\n")){
                bClass.add(item);
            }
//            Log.e("tgrom", "initConfig over");
        }
        catch(Exception ex){
            Log.e("tgrom", "initConfig err:" + ex.getMessage());
            return ;
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
                if (out != null){
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void WriteConfig(String path,String jspath,int port){
        try {
            FileWriter writer= new FileWriter(path);
            String fconfig="";
            if(jspath.equals("listen")){
                fconfig="{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"listen\",\n" +
                        "    \"address\": \"0.0.0.0\",\n" +
                        "    \"port\": "+port+",\n" +
                        "    \"on_load\": \"resume\"\n" +
                        "  }\n" +
                        "}";
            }else if(jspath.equals("listen_wait")){
                fconfig="{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"listen\",\n" +
                        "    \"address\": \"0.0.0.0\",\n" +
                        "    \"port\": "+port+",\n" +
                        "    \"on_load\": \"wait\"\n" +
                        "  }\n" +
                        "}";
            }else{
                String processName = ActivityThread.currentProcessName();
                String fName = jspath.trim();
                String fileName = fName.substring(fName.lastIndexOf("/")+1);
                String newJsPath="/data/data/"+processName+"/"+fileName;
                mycopy(jspath, newJsPath);
                int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
                FileUtils.setPermissions(newJsPath, perm, -1, -1);//将权限改为777
                fconfig="{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"script\",\n" +
                        "    \"path\": \""+newJsPath+"\"\n" +
                        "  }\n" +
                        "}";
            }
            Log.e("tgrom", "WriteConfig config:"+fconfig);
            writer.write(fconfig);
            writer.close();
        } catch (IOException e) {
            Log.e("tgrom", "WriteConfig err:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadSo(String path){
        String processName = ActivityThread.currentProcessName();
        String fName = path.trim();
        String fileName = fName.substring(fName.lastIndexOf("/")+1);
        String tagPath = "/data/data/" + processName + "/"+fileName;//64位so的目录
        mycopy(path, tagPath);
        int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
        FileUtils.setPermissions(tagPath, perm, -1, -1);//将权限改为777
        File file = new File(tagPath);
        if (file.exists()){
            Log.e("tgrom", "load so:"+tagPath);
            System.load(tagPath);
            file.delete();//用完就删否则不会更新
        }
    }

    //注入so
    public static void loadConfigSo(){
        String processName = ActivityThread.currentProcessName();
        for(PackageItem item : mikConfigs){
            if(!item.packageName.equals(processName))
                continue;
            if(item.soPath.length()<=0)
                continue;

            if(item.isDobby){
                if(System.getProperty("os.arch").indexOf("64") >= 0) {
                    loadSo("/system/lib64/libdby_64.so");
                }else{
                    loadSo("/system/lib/libdby.so");
                }
            }
            String[] soList=item.soPath.split("\n");
            for(String sopath :soList){
                loadSo(sopath);
            }
        }
    }

    private static DexClassLoader loadDex(String path,Application app) {
        String fName = path.trim();
        String processName = ActivityThread.currentProcessName();
        String fileName = fName.substring(fName.lastIndexOf("/")+1);
        String tagPath = "/data/data/" + processName + "/"+fileName;//64位so的目录
        mycopy(path, tagPath);
        int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
        FileUtils.setPermissions(tagPath, perm, -1, -1);//将权限改为777
        File file = new File(tagPath);
        if (file.exists()){
            DexClassLoader dexClassLoader = new DexClassLoader(
                    tagPath,//dex路径
                    app.getCacheDir().toString(),//优化之后的文件的路径
                    app.getPackageResourcePath(),//原生库路径
                    app.getClassLoader()//父类加载器
            );
            return dexClassLoader;
        }
        return null;
    }

    //注入dex
    public static void loadConfigDex(Application app){
        String processName = ActivityThread.currentProcessName();
        for(PackageItem item : mikConfigs){
            if(!item.packageName.equals(processName))
                continue;
            if(item.dexPath.length()<=0)
                continue;
            String[] dexList=item.dexPath.split("\n");
            for(String dexpath :dexList){
                DexClassLoader dexClassLoader= loadDex(dexpath,app);
                Class clzz = null;
                try {
                    if(item.dexClassName.length()>=0){
                        clzz = dexClassLoader.loadClass(item.dexClassName);
                    }else{
                        clzz = dexClassLoader.loadClass("cn.mik.InjectDex");
                    }
                    IMikDex lib = (IMikDex)clzz.newInstance();
                    lib.onStart();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //加载frida-gadget,实现持久化frida
    public static void loadGadget(){
        String processName = ActivityThread.currentProcessName();
        for(PackageItem item : mikConfigs){
            if(item.packageName.equals(processName)){
                try{
                    boolean flag=true;
                    if(item.fridaJsPath.length()<=0){
                        continue;
                    }
                    String configPath="/data/data/"+processName+"/libfdgg.config.so";
                    String configPath2="/data/data/"+processName+"/libfdgg32.config.so";
                    if(item.fridaJsPath.equals("listen")||item.fridaJsPath.equals("listen_wait")){
                        WriteConfig(configPath,item.fridaJsPath,item.port);
                        WriteConfig(configPath2,item.fridaJsPath,item.port);
                    }else{
                        File file = new File(item.fridaJsPath);
                        if(!file.exists()){
                            file = new File( "/data/data/" + processName +"/"+file.getName());
                        }
                        if(!file.exists()){
                            Log.e("tgrom", "initConfig package:" + processName+" frida js path:"+item.fridaJsPath+" not found");
                            continue;
                        }
                        WriteConfig(configPath,item.fridaJsPath,item.port);
                        WriteConfig(configPath2,item.fridaJsPath,item.port);
                    }
                    String tagPath = "/data/data/" + processName + "/libfdgg.so";//64位so的目录
                    String tagPath2 = "/data/data/" + processName + "/libfdgg32.so";//32位的so目录

                    if(item.gadgetPath!=null&&item.gadgetPath.length()>0){
                        mycopy(item.gadgetArm64Path, tagPath);//复制so到私有目录
                        mycopy(item.gadgetPath, tagPath2);
                    }else{
                        mycopy("/system/lib64/libfdgg.so", tagPath);//复制so到私有目录
                        mycopy("/system/lib/libfdgg.so", tagPath2);
                    }
                    int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
                    FileUtils.setPermissions(tagPath, perm, -1, -1);//将权限改为777
                    FileUtils.setPermissions(tagPath2, perm, -1, -1);
                    FileUtils.setPermissions(configPath, perm, -1, -1);
                    FileUtils.setPermissions(configPath2, perm, -1, -1);
                    File file1 = new File(tagPath);
                    File file2 = new File(tagPath2);
                    if (file1.exists()) {
                        Log.e("tgrom", "app: " +System.getProperty("os.arch"));//判断是64位还是32位
                        if (System.getProperty("os.arch").indexOf("64") >= 0) {
                            Log.e("tgrom", "initConfig package:" + processName+" frida js path:"+item.fridaJsPath+" load arch64");
                            System.load(tagPath);
                            file1.delete();//用完就删否则不会更新
                        } else {
                            Log.e("tgrom", "initConfig package:" + processName+" frida js path:"+item.fridaJsPath+" load 32");
                            System.load(tagPath2);
                            file2.delete();
                        }
                    }
                    Log.e("tgrom", "initConfig package:" + processName+" initConfig over");
                }catch(Exception ex){
                    Log.e("tgrom", "initConfig package:" + processName+" frida js path:"+item.fridaJsPath+" load err:"+ex.getMessage());
                }
                break;
            }
        }
    }

    public static boolean shouldtgRom() {
        boolean should = false;
        String processName = ActivityThread.currentProcessName();
        Log.e("tgrom", "shouldtgRom processName:"+processName);
        for(PackageItem item : mikConfigs){
            if(item.packageName.equals(processName)){
                Log.e("tgrom", "shouldtgRom SetRomConfig");
                SetRomConfig(item);
                if(item.isTuoke){
                    should=true;
                    if(item.breakClass.length()>0){
                        Log.e("tgrom", "shouldtgRom breakClass:"+item.breakClass);
                        String[] bclasses=item.breakClass.split("\n");
                        for(String cls : bclasses){
                            bClass.add(cls);
                        }
                    }
                    if(item.whiteClass.length()>0){
                        Log.e("tgrom", "shouldtgRom whiteClass:"+item.whiteClass);
                        String[] wclasses=item.whiteClass.split("\n");
                        for(String cls : wclasses){
                            whiteClass.add(cls);
                        }
                    }
                    whitePath=item.whitePath;
                }
                break;
            }
        }
        Log.e("tgrom", "shouldtgRom processName:"+processName+" res:"+should);
        return should;
    }

    public static String getClassList() {
        String processName = ActivityThread.currentProcessName();
        BufferedReader br = null;
        if(whitePath.length()<=0){
            Log.e("tgrom", "shouldtgRom processName:"+processName+" not whitePath");
            return "";
        }
        Log.e("tgrom", "shouldtgRom processName:"+processName+" whitePath:"+whitePath);
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(whitePath));
            String line;
            while ((line = br.readLine()) != null) {

                if (line.length() >= 2) {
                    sb.append(line + "\n");
                }
            }
            br.close();
        } catch (Exception ex) {
            Log.e("tgrom", "getClassList err:" + ex.getMessage());
            return "";
        }
        return sb.toString();
    }

    public static void tgWithClassList (String classlist){
        Log.e("tgrom", "tgWithClassList");
        ClassLoader appClassloader = getClassloader();
        if (appClassloader == null) {
            Log.e("tgrom", "appClassloader is null");
            return;
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Method dumpMethodCode_method = null;
        Method dumpRepair_method=null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("tgextMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
            if (field.getName().equals("dumpRepair")) {
                dumpRepair_method = field;
                dumpRepair_method.setAccessible(true);
            }
        }
        String[] classes = classlist.split("\n");
        for (String clsname : classes) {
            String line = clsname;
            if (line.startsWith("L") && line.endsWith(";") && line.contains("/")) {
                line = line.substring(1, line.length() - 1);
                line = line.replace("/", ".");
            }
            loadClassAndInvoke(appClassloader, line, dumpMethodCode_method);
        }
        if(dumpRepair_method!=null){
            Log.e("tgrom", "tgWithClassList dumpRepair");
            try {
                dumpRepair_method.invoke(null);
            }catch(Exception ex){
                Log.e("tgrom", "tgWithClassList dumpRepair invoke err:"+ex.getMessage());
            }

        }else{
            Log.e("tgrom", "tgWithClassList dumpRepair is null");
        }

    }

    public static void tgunpackerThread () {

        if (!shouldtgRom()) {
            return;
        }

        String classlist = getClassList();
        if (!classlist.equals("")) {
            tgWithClassList(classlist);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Log.e("tgrom", "start sleep......");
                    Thread.sleep(1 * 30 * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Log.e("tgrom", "sleep over and start tgUnpacker");
                tgUnpacker();
                Log.e("tgrom", "tgUnpacker run over");
            }
        }).start();
    }

}

