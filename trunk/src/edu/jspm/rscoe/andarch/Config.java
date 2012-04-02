package edu.jspm.rscoe.andarch;

enum application_mode {
	pure_ar, hybrid, pure_vr
}
public class Config {
	public final static boolean DEBUG = false;
	public static application_mode appMode = application_mode.pure_ar;
}