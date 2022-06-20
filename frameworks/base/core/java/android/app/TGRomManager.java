package android.app;

import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;

// add
@SystemService(Context.TGROM_SERVICE)
public class TGRomManager {
    Context mContext;
    ITGRom mService;

    public TGRomManager(Context context,ITGRom service){
        if(service==null){
            Slog.e("MikRomManager","Construct service is null");
        }
        mContext = context;
        mService = service;
    }

    public String shellExec(String cmd){
        if(mService != null){
            try{
                Slog.e("MikRomManager","shellExec");
                return mService.shellExec(cmd);
            }catch(RemoteException e){
                Slog.e("MikRomManager","RemoteException "+e);
            }
        }else{
            Slog.e("MikRomManager","mService is null");
        }
        return "";
    }

    public String readFile(String path){
        if(mService != null){
            try{
                Slog.e("MikRomManager","readFile");
                return mService.readFile(path);
            }catch(RemoteException e){
                Slog.e("MikRomManager","RemoteException "+e);
            }
        }else{
            Slog.e("MikRomManager","mService is null");
        }
        return "";
    }

    public void writeFile(String path,String data){
        if(mService != null){
            try{
                Slog.e("MikRomManager","writeFile");
                mService.writeFile(path,data);
            }catch(RemoteException e){
                Slog.e("MikRomManager","RemoteException "+e);
            }
        }else{
            Slog.e("MikRomManager","mService is null");
        }
    }

}
