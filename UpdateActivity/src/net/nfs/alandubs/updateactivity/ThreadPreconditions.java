package net.nfs.alandubs.updateactivity;

import android.os.Looper;

//Checks whether running in main thread.
//Should this go nested in the calling class?

public class ThreadPreconditions {

	public static void checkOnMainThread(){
		if(BuildConfig.DEBUG){
			if(Thread.currentThread() != Looper.getMainLooper().getThread()){
				throw new IllegalStateException("This method should be called from the Main Thread, man");
			}
		}
	}

}
