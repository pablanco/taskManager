package com.artech.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.util.Log;

import com.artech.base.services.Services;

public class ZipHelper {
  private String _zipFile;
  private String _location;
  private InputStream _input;

  public ZipHelper(String zipFile, String location) {
    _zipFile = zipFile;
    _location = location;

    _dirChecker("");
  }
  
  public ZipHelper(InputStream zipFile) {
	  _input = zipFile;
  }

  public void unzip(Context context) {
    try  {
      
      InputStream fin = (_input == null)? new FileInputStream(_zipFile) : new BufferedInputStream(_input);
      
      ZipInputStream zin = new ZipInputStream(fin);
      ZipEntry ze;
      final byte buffer[] = new byte[200000];
      while ((ze = zin.getNextEntry()) != null) {
        Log.v("Decompress", "Unzipping " + ze.getName());

        if(ze.isDirectory()) {
          _dirChecker(ze.getName());
        } else {
          FileOutputStream fout = context.openFileOutput(Services.Application.getName() + ze.getName().replace(".json", ""), Context.MODE_PRIVATE);
          for (int c = zin.read(buffer, 0, buffer.length); c != -1; c = zin.read(buffer, 0, buffer.length)) {
            fout.write(buffer, 0, c);
          }

          zin.closeEntry();
          fout.close();
        }
        
      }
      zin.close();
    } catch(Exception e) {
      Log.e("Decompress", "unzip", e);
    }
  }

  private void _dirChecker(String dir) {
    File f = new File(_location + dir);

    if(!f.isDirectory()) {
      f.mkdirs();
    }
  }
}