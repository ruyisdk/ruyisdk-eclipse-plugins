package org.ruyisdk.devices.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.InputStream;
import java.io.OutputStream;
import org.ruyisdk.core.basedir.XdgDirs;
import org.ruyisdk.devices.model.Device;
import org.ruyisdk.core.console.RuyiSdkConsole;
import org.ruyisdk.core.config.Constants;

public class PropertiesService {
	private static final Path FILE_PATH = Paths.get(XdgDirs.getConfigDir(Constants.AppInfo.AppDir).toString(), Constants.ConfigFile.DeviceProperties);  //devices.properties
	private static final String DEFAULT_DEVICE_KEY = "default_device";
	 
	public List<Device> loadDevices() {
		List<Device> devices = new ArrayList<>();
		Properties properties = new Properties();

		// 读取文件内容到Properties对象
		try (InputStream inputStream = Files.newInputStream(FILE_PATH)) {
			properties.load(inputStream);

			String defaultBoard = properties.getProperty(DEFAULT_DEVICE_KEY);
			int index = 1;
			while (properties.containsKey("device." + index + ".name")) {
				String name = properties.getProperty("device." + index + ".name");
				String chip = properties.getProperty("device." + index + ".chip");
				String vendor = properties.getProperty("device." + index + ".vendor");
				String version = properties.getProperty("device." + index + ".version");
				boolean isDefault = ("device." + index).equals(defaultBoard);

				devices.add(new Device(name, chip, vendor, version, isDefault));
				index++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return devices;
	}
	
//    public Device getDefaultBoard(List<Device> devices) {
//        Properties props = new Properties();
//        
//        try (InputStream input = new FileInputStream(FILE_PATH.toString())) {
//            props.load(input);
//            String defaultBoardKey = props.getProperty(DEFAULT_DEVICE_KEY);
//            
//            if (defaultBoardKey != null) {
//                int defaultIndex = Integer.parseInt(defaultBoardKey.split("\\.")[1]);
//                if (defaultIndex > 0 && defaultIndex <= devices.size()) {
//                    return devices.get(defaultIndex - 1);
//                }
//            }
//        } catch (IOException | NumberFormatException e) {
//            // Return null if no default board set
//        }
//        
//        return null;
//    }

	public void saveDevices(List<Device> devices) {
		Properties properties = new Properties();

		int index = 1;
		for (Device device : devices) {
			properties.setProperty("device." + index + ".name", device.getName());
			properties.setProperty("device." + index + ".chip", device.getChip());
			properties.setProperty("device." + index + ".vendor", device.getVendor());
			properties.setProperty("device." + index + ".version", device.getVersion());
			if (device.isDefault()) {
				properties.setProperty(DEFAULT_DEVICE_KEY, "device." + index);
			}

			index++;
		}
		
		// 使用转换后的路径创建 FileOutputStream
		try (OutputStream output = new FileOutputStream(FILE_PATH.toString())) {
			properties.store(output, "RuyiSDK Devices Configuration");
			
			RuyiSdkConsole.getInstance().logInfo("Devices is successfully stored to file :"+FILE_PATH.toString());
			
		} catch (IOException e) {
			e.printStackTrace();
			
			RuyiSdkConsole.getInstance().logError("Devices storage failure!");
		} 
	}
}

