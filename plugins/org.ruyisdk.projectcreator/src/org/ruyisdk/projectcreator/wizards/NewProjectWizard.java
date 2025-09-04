package org.ruyisdk.projectcreator.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Enumeration;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.Bundle;
import org.ruyisdk.projectcreator.Activator;
import org.ruyisdk.projectcreator.natures.MyProjectNature;
import org.ruyisdk.projectcreator.utils.ToolchainLocator;

public class NewProjectWizard extends Wizard implements INewWizard {

    private BoardSelectionPage boardSelectionPage;
    private ProjectSettingsPage projectSettingsPage;
    private IWorkbench workbench;

    public NewProjectWizard() {
        setWindowTitle("new RuyiSDK Project");
        setNeedsProgressMonitor(true);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
    }

    @Override
    public void addPages() {
        boardSelectionPage = new BoardSelectionPage("Select Board");
        projectSettingsPage = new ProjectSettingsPage("Project Settings");
        addPage(boardSelectionPage);
        addPage(projectSettingsPage);
    }

    @Override
    public boolean canFinish() {
        return (getContainer().getCurrentPage() == projectSettingsPage && projectSettingsPage.isPageComplete());
    }

    @Override
    public boolean performFinish() {
        final String boardModel = boardSelectionPage.getBoardModel();
        final String projectName = projectSettingsPage.getProjectName();
        final String toolchainPath = projectSettingsPage.getToolchainPath();
        final String cflags = projectSettingsPage.getCFlags(); // 1. Get CFLAGS from settings page

        if (toolchainPath == null || toolchainPath.trim().isEmpty()) {
            MessageDialog.openError(getShell(), "Error", "toolchainPath Cannot be empty");
            return false;
        }

        String templateToUse = boardModel;
        Bundle bundle = Activator.getDefault().getBundle();
        URL templateDirUrl = bundle.getEntry("/templates/" + boardModel);

        if (templateDirUrl == null) {
            boolean useDefault = MessageDialog.openConfirm(getShell(), "Template Not Found",
                            boardModel + "\" demo is not found\n\nare you want to use the default template?");

            if (useDefault) {
                templateToUse = "default";
            } else {
                return false;
            }
        }

        final String finalTemplateName = templateToUse;
        IRunnableWithProgress op = monitor -> {
            try {
                // 2. Pass CFLAGS to the createProject method
                createProject(projectName, boardModel, toolchainPath, cflags, finalTemplateName, monitor);
            } catch (CoreException | IOException e) {
                throw new InvocationTargetException(e);
            } finally {
                monitor.done();
            }
        };

        try {
            getContainer().run(true, false, op);
            ToolchainLocator.saveLastUsedToolchainPath(toolchainPath);
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.openError(getShell(), "create project failed", e.getMessage());
            return false;
        }
        return true;
    }

    public String getBoardModel() {
        if (boardSelectionPage != null) {
            return boardSelectionPage.getBoardModel();
        }
        return "";
    }

    // 3. Update method signature to accept CFLAGS
    private void createProject(String projectName, String boardModel, String toolchainPath, String cflags,
                    String templateName, IProgressMonitor monitor) throws CoreException, IOException {
        monitor.beginTask("Creating project " + projectName, 4);

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

        project.create(monitor);
        project.open(monitor);
        monitor.worked(1);

        IProjectDescription description = project.getDescription();

        description.setBuildSpec(new ICommand[0]);

        description.setNatureIds(new String[] {MyProjectNature.NATURE_ID});

        project.setDescription(description, monitor);
        monitor.worked(1);
        // 4. Pass CFLAGS to the copyTemplateFiles method
        copyTemplateFiles(project, templateName, toolchainPath, cflags, monitor);
        monitor.worked(1);

        project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, "boardModel"), boardModel);
        project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, "toolchainPath"), toolchainPath);
        project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, "buildCmd"), "make");
        project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, "cflags"), cflags); // 5. Save CFLAGS as a
                                                                                                 // persistent
                                                                                                 // property
        // refresh the project to ensure all changes are applied
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        monitor.worked(1);
    }

    // 6. Update method signature to accept CFLAGS
    private void copyTemplateFiles(IProject project, String templateName, String toolchainRootPath, String cflags,
                    IProgressMonitor monitor) throws CoreException, IOException {
        Bundle bundle = Activator.getDefault().getBundle();
        String templatePath = "/templates/" + templateName;
        Enumeration<URL> entries = bundle.findEntries(templatePath, "*", true);

        if (entries == null) {
            throw new CoreException(
                            new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Template not found: " + templatePath));
        }

        while (entries.hasMoreElements()) {
            URL entryUrl = entries.nextElement();
            String entryPath = entryUrl.getPath();
            String targetPath = entryPath.substring(templatePath.length());

            if (targetPath.isEmpty() || targetPath.equals("/")) {
                continue;
            }

            if (entryPath.endsWith("/")) {
                IFolder folder = project.getFolder(targetPath);
                if (!folder.exists()) {
                    folder.create(true, true, monitor);
                }
                continue;
            }

            IFile file = project.getFile(targetPath);

            if (file.getName().equals("Makefile")) {
                String toolchainBinPath = Paths.get(toolchainRootPath, "bin").toString();
                String toolchainPrefixName = findToolchainPrefixName(toolchainRootPath);

                if (toolchainPrefixName == null) {
                    toolchainPrefixName = "riscv64-unknown-elf";
                    System.err.println("Warning: Failed to infer toolchain prefix name from '" + toolchainRootPath
                                    + "', using default value: " + toolchainPrefixName);
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(entryUrl.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.replace("__TOOLCHAIN_BIN_PATH__", toolchainBinPath);
                        line = line.replace("__TOOLCHAIN_PREFIX_NAME__", toolchainPrefixName);
                        line = line.replace("__CFLAGS_OPTIONS__", cflags); // 7. Replace CFLAGS placeholder
                        sb.append(line).append(System.lineSeparator());
                    }
                }

                String finalContent = sb.toString();
                try (InputStream newContentStream =
                                new ByteArrayInputStream(finalContent.getBytes(StandardCharsets.UTF_8))) {
                    if (file.exists()) {
                        file.setContents(newContentStream, true, true, monitor);
                    } else {
                        file.create(newContentStream, true, monitor);
                    }
                }

            } else {
                try (InputStream is = entryUrl.openStream()) {
                    if (file.exists()) {
                        file.setContents(is, true, true, monitor);
                    } else {
                        file.create(is, true, monitor);
                    }
                }
            }
        }
    }

    private String findToolchainPrefixName(String toolchainRootPath) {
        File binDir = new File(toolchainRootPath, "bin");
        if (!binDir.isDirectory()) {
            return null;
        }

        File[] files = binDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith("-gcc") && file.isFile()) {
                    return name.substring(0, name.length() - "-gcc".length());
                }
            }
        }
        return null;
    }
}
