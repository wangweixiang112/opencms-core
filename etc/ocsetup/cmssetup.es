setPrompt();
cms.loginUser("Admin","admin");
cms.createPropertydefinition("Title","plain");
cms.createPropertydefinition("Title","compatiblePlain");
cms.createPropertydefinition("Title","page");
cms.createPropertydefinition("Title","XMLTemplate");
cms.createPropertydefinition("Title","binary");
cms.createPropertydefinition("Title","script");
cms.createPropertydefinition("Title","image");
cms.createPropertydefinition("Title","link");
cms.createPropertydefinition("NavText","page");
cms.createPropertydefinition("NavPos","page");
cms.createPropertydefinition("NavText","plain");
cms.createPropertydefinition("NavPos","plain");
cms.createPropertydefinition("NavText","compatiblePlain");
cms.createPropertydefinition("NavPos","compatiblePlain");
cms.createPropertydefinition("NavText","folder");
cms.createPropertydefinition("NavPos","folder");
cms.createPropertydefinition("Title","folder");
cms.createPropertydefinition("Description","folder");
cms.createPropertydefinition("Keywords","folder");
cms.createPropertydefinition("Title","pdfpage");
cms.createPropertydefinition("NavText","pdfpage");
cms.createPropertydefinition("NavPos","pdfpage");
cms.createPropertydefinition("TemplateType","plain");
cms.createPropertydefinition("TemplateType","compatiblePlain");
cms.createPropertydefinition("TemplateType","pdfpage");
cms.createPropertydefinition("TemplateType","XMLTemplate");
cms.createPropertydefinition("Description","page");
cms.createPropertydefinition("Keywords","page");
cms.createPropertydefinition("Description","plain");
cms.createPropertydefinition("Keywords","plain");
cms.createPropertydefinition("Description","compatiblePlain");
cms.createPropertydefinition("Keywords","compatiblePlain");
cms.createPropertydefinition("Description","script");
cms.createPropertydefinition("Keywords","script");
cms.createPropertydefinition("module","compatiblePlain");
cms.createPropertydefinition("Keywords","binary");
cms.createPropertydefinition("Description","binary");
cms.addFileExtension("doc","binary");
cms.addFileExtension("pdf","binary");
cms.addFileExtension("zip","binary");
cms.addFileExtension("class","binary");
cms.addFileExtension("txt","plain");
cms.addFileExtension("html","plain");
cms.addFileExtension("htm","plain");
cms.addFileExtension("jpeg","image");
cms.addFileExtension("jpg","image");
cms.addFileExtension("gif","image");
cms.addFileExtension("js","script");
cms.getRequestContext().setCurrentProject(2);
cms.importResources("ocsetup/vfs","/");
cms.unlockProject(2);
cms.publishProject(2);
cms.writeExportPath("export/");
exit();
