package luo.uvc.jni;

public abstract interface MyRunnable extends Runnable
{
	
	
	//�Ƿ�����߳�
	public void setSuspend(boolean susp);
	
	public boolean isSuspend();
	
	public void runPesonelLogic();
}
