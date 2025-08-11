package org.ruyisdk.devices;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.ruyisdk.core.console.ConsoleManager;
import org.ruyisdk.core.console.RuyiSdkConsole;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.ruyisdk.devices"; 

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
//		// (可选)启动时自动打开控制台
//        ConsoleManager.showConsole();
        
        RuyiSdkConsole.getInstance().logInfo("Devices Plugin " + getBundle().getVersion()+" Activated !");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		
		 ConsoleManager.dispose();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
