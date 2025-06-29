package org.ruyisdk.projectcreator.builder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.ruyisdk.projectcreator.Activator;

public class MakefileBuilder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = "org.ruyisdk.projectcreator.makefileBuilder";
    private static final String BUILD_CMD_PROPERTY = "buildCmd";
    private static final String LOG_FILE_NAME = "build_output.log";

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        IProject project = getProject();

        String buildCommand = project.getPersistentProperty(
            new QualifiedName(Activator.PLUGIN_ID, BUILD_CMD_PROPERTY)
        );

        if (buildCommand == null || buildCommand.trim().isEmpty()) {
            buildCommand = "make";
        }
        
        System.out.println(">>> MakefileBuilder: Using build command: " + buildCommand);

        clearLogFile(project);
        logToFile(project, "=== Build started at " + new Date() + " ===");

        logToFile(project, "Build command: " + buildCommand);
        logToFile(project, "Project: " + project.getName());
        logToFile(project, "Location: " + project.getLocation());

        try {

            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand.split("\\s+"));
            processBuilder.directory(project.getLocation().toFile());
            processBuilder.redirectErrorStream(true);
            logToFile(project, "Working directory: " + processBuilder.directory().getAbsolutePath());

            Process process = processBuilder.start();

            // 实时读取并记录输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(">>> MakefileBuilder: [BUILD] " + line);
                    logToFile(project, line);
                }
            }

            // 等待进程完成并记录结果
            int exitCode = process.waitFor();
            String resultMessage = "Build finished with exit code: " + exitCode + " at " + new Date();
            logToFile(project, resultMessage);

        } catch (Exception e) {
            String errorMessage = "Build failed with exception: " + e.getMessage();
            System.err.println(">>> MakefileBuilder: " + errorMessage);
            e.printStackTrace();
            logToFile(project, errorMessage);
        } finally {
            monitor.done();
        }

        // refresh the project to ensure all changes are visible
        project.refreshLocal(IProject.DEPTH_INFINITE, monitor);
        return null;
    }

    private String getBuildKindString(int kind) {
        switch (kind) {
            case AUTO_BUILD: return "AUTO_BUILD";
            case FULL_BUILD: return "FULL_BUILD";
            case INCREMENTAL_BUILD: return "INCREMENTAL_BUILD";
            case CLEAN_BUILD: return "CLEAN_BUILD";
            default: return "UNKNOWN (" + kind + ")";
        }
    }
        private void clearLogFile(IProject project) {
        IFile logFile = project.getFile(LOG_FILE_NAME);
        try {
            if (logFile.exists()) {
                logFile.delete(true, null);
            }
        } catch (CoreException e) {
            System.err.println(">>> MakefileBuilder: Failed to clear log file: " + e.getMessage());
        }
    }
   
    // writes a message to the project's log file.
    private void logToFile(IProject project, String message) {
        IFile logFile = project.getFile(LOG_FILE_NAME);
        try {
            // If the file does not exist, create it
            if (!logFile.exists()) {
                try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
                    logFile.create(stream, true, null);
                }
            }
            
            // add the message to the log file
            try (FileWriter writer = new FileWriter(logFile.getLocation().toFile(), true);
                 PrintWriter printWriter = new PrintWriter(writer)) {
                printWriter.println(message);
            }
        } catch (IOException | CoreException e) {
            System.err.println(">>> MakefileBuilder: Failed to write to log file: " + e.getMessage());
        }
    }


}