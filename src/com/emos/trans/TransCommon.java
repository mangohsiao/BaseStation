package com.emos.trans;

public class TransCommon {
	/*
	 * Time value
	 */

	/* ��¼��ʱʱ�䣬�����ʱ�����û�е�¼�����ս�session */
	public static final int PHONE_HEART_BEAT_ITVL = 25000;
	public static final int PHONE_SO_READ_TIMEOUT = 50000;
	
	/* ��¼��ʱʱ�䣬�����ʱ�����û�е�¼�����ս�session */
	public static final int DELAY_LOGIN_CHECK_TASK = 8000;	
	/* ��ȡ���ݳ�ʱʱ�䣬�����ʱ�����û�ж�ȡ����������session�ս� */
	public static final int READER_IDEL_TIMEOUT = 40000;	

	public static final int MHOLDER_TYPE_PHONE = 2;
	public static final int MHOLDER_TYPE_HOME = 2;
	
	/*
	 * Message for Phone
	 */

	/* Login Message */
	public static final int MSG_PHONE_LOGIN = 0x11;
	public static final int MSG_PHONE_LOGIN_RE = 0x12;
	
	/* Login Message */
	public static final int MSG_PHONE_LOGOUT = 0x13;
	public static final int MSG_PHONE_LOGOUT_RE = 0x14;
	
	/* Register UUID Message */
	public static final int MSG_PHONE_REG_UUID = 0x15;
	public static final int MSG_PHONE_REG_UUID_RE = 0x16;

	/* Un-Register UUID Message */
	public static final int MSG_PHONE_UNREG_UUID = 0x17;
	public static final int MSG_PHONE_UNREG_UUID_RE = 0x18;
	
	
	/*
	 * Message for Home
	 */

	/* 0x21 Home���״����ӷ��ͣ�UUIDȷ�ϡ� */
	public static final int MSG_HOME_REG = 0x21;
	public static final int MSG_HOME_REG_RE = 0x22;
	
	/* 0x23 Home�˶Ͽ����ӷ��ͣ�UUID��Map��ȥ���� */
	public static final int MSG_HOME_UNREG = 0x23;
	public static final int MSG_HOME_UNREG_RE = 0x24;

	
	/*
	 * Message for PUSH
	 */
	
	/* 0x31 �������� */
	public static final int MSG_HOME_ALARM = 0x31;
	public static final int MSG_HOME_ALARM_RE = 0x32;
	public static final int MSG_PUSH_ALARM = 0x33;
	public static final int MSG_PUSH_ALARM_RE = 0x34;
	
	/* 0x35 �������� */
	public static final int MSG_HOME_UPDATE = 0x35;
	public static final int MSG_HOME_UPDATE_RE = 0x36;
	public static final int MSG_PUSH_UPDATE = 0x37;
	public static final int MSG_PUSH_UPDATE_RE = 0x38;
	
	/* 0x41 ͬ������ */
	public static final int MSG_HOME_SYNC = 0x41;
	public static final int MSG_HOME_SYNC_RE = 0x42;
	public static final int MSG_PUSH_SYNC = 0x43;
	public static final int MSG_PUSH_SYNC_RE = 0x44;
	
	/*
	 * Code for RES
	 */
	public static final int RES_HOME_NOT_REG = 0x401;
}
