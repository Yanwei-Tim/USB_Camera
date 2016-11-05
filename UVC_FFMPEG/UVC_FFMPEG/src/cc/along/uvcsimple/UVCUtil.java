package cc.along.uvcsimple;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.util.Log;

public class UVCUtil {
	
	public static void listDevFile(Context context){
		File file = new File("/dev");
		Builder alertDialog = new AlertDialog.Builder(context);
		alertDialog.setTitle("Dev�ļ����б�");
		ArrayList<String> list = new ArrayList<String>();
		
		if(file.isDirectory()){
			File[] files = file.listFiles();
			for(File f: files){
//				Log.i(TAG, "Dev file: " + f.getName());
				list.add(f.getName());
			}
		}
    	String[] str = (String[]) list.toArray(new String[0]);
		alertDialog.setItems(str, null);
		alertDialog.create().show();
	}
}
