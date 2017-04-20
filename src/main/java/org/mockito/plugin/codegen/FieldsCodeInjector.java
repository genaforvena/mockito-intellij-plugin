package org.mockito.plugin.codegen;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Inserts code with declaration of fields that can be auto-generated in a Mockito test:
 * - mocked fields
 * - subject of the test
 *
 * Mocked fields are inserted for each non-static object defined in the tested class. So far the fields
 * declared in parent of the tested class are ignored. Example of the code generated for an object of type ClassName:
 * <code>
 *     @Mock
 *     private ClassName className;
 * </code>
 *
 * Field for a subject of the test has format:
 * <code>
 *     @InjectMocks
 *     private SubjectClassName underTest;
 * </code>
 *
 * Created by przemek on 8/9/15.
 */
public class FieldsCodeInjector implements CodeInjector {

    public static final String TEST_CLASS_NAME_SUFFIX = "Test";
    public static final String INJECT_MOCKS_ANNOTATION_QUALIFIED_NAME = "org.mockito.InjectMocks";
    public static final String MOCK_ANNOTATION_QUALIFIED_NAME = "org.mockito.Mock";
    public static final String MOCK_ANNOTATION_SHORT_NAME = "Mock";
    public static final String UNDER_TEST_FIELD_NAME = "mUnderTest";

    private final PsiJavaFile psiJavaFile;
    private final Project project;
    private final JavaPsiFacade javaPsiFacade;
    private final ImportOrganizer importOrganizer;
    private final JavaCodeStyleManager codeStyleManager;

    public FieldsCodeInjector(PsiJavaFile psiJavaFile, ImportOrganizer importOrganizer) {
        this.psiJavaFile = psiJavaFile;
        this.project = psiJavaFile.getProject();
        this.javaPsiFacade = JavaPsiFacade.getInstance(project);
        this.importOrganizer = importOrganizer;
        this.codeStyleManager =  JavaCodeStyleManager.getInstance(project);
    }

    @Override
    public void inject() {
        PsiClass psiClass = MockitoPluginUtils.getUnitTestClass(psiJavaFile);
        Set<String> existingFieldTypeNames = getFieldTypeNames(psiClass);

        String underTestQualifiedClassName = getUnderTestQualifiedClassName(psiClass);
        if (underTestQualifiedClassName == null) {
            return;
        }

        insertMockedFields(underTestQualifiedClassName, psiClass, existingFieldTypeNames);

        insertUnderTestField(psiClass, existingFieldTypeNames, underTestQualifiedClassName);
    }

    private void insertUnderTestField(PsiClass psiClass, Set<String> existingFieldTypeNames,
                                      String underTestQualifiedClassName) {
        if (!existingFieldTypeNames.contains(underTestQualifiedClassName)) {
            insertUnderTestField(psiClass, underTestQualifiedClassName);
            importOrganizer.addClassImport(psiJavaFile, INJECT_MOCKS_ANNOTATION_QUALIFIED_NAME);
        }
    }

    private void insertMockedFields(String underTestQualifiedClassName, PsiClass psiClass,
                                    Set<String> existingFieldTypeNames) {
        PsiClass underTestPsiClass = javaPsiFacade.findClass(
                underTestQualifiedClassName, GlobalSearchScope.allScope(project));
        if (underTestPsiClass == null) {
            return;
        }
        boolean addedMocks = false;
        for (PsiMethod psiMethod : underTestPsiClass.getConstructors()) {
            if (AnnotationUtil.isAnnotated(psiMethod, "Inject", false)) {
                PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
                for (PsiParameter parameter : parameters) {
                    PsiType type = parameter.getTypeElement().getType();
                    insertMockedField(psiClass, type);
                    addedMocks = true;
                }

                insertSetUp(psiClass, psiMethod);
                break;
            }
        }
        if (addedMocks) {
            importOrganizer.addClassImport(psiJavaFile, MOCK_ANNOTATION_QUALIFIED_NAME);
        }
    }

    private void insertSetUp(PsiClass psiClass, PsiMethod psiMethod) {
        PsiElementFactory elementFactory = PsiElementFactory.SERVICE.getInstance(project);

        PsiMethod methodFromText = elementFactory.createMethodFromText(
                "@Before\n" +
                        "public void setUp() {\n" +
                        "initMocks(this);\n" +
                        "\n" +
                        "}\n", null);

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
        List<String> arguments = new ArrayList<>();
        for (PsiParameter parameter : parameters) {
            PsiType type = parameter.getTypeElement().getType();
            arguments.add(suggestFieldName(type));
        }

        String delim = "";
        StringBuilder sb = new StringBuilder();
        for (String i : arguments) {
            sb.append(delim).append(i);
            delim = ",";
        }
        String argumentsString = sb.toString();
        methodFromText.getBody().add(elementFactory.createStatementFromText("mUnderTest = new Foo(" +
                "" + argumentsString +
                ");", null));

        psiClass.add(methodFromText);
    }

    /**
     * Returns fully qualified names of the fields declared in the class, ignores fields inherited from parent classes.
     */
    @NotNull
    private Set<String> getFieldTypeNames(PsiClass psiClass) {
        Set<String> existingFieldTypeNames = new HashSet<>();
        for (PsiField psiField : psiClass.getFields()) {
            existingFieldTypeNames.add(psiField.getType().getCanonicalText());
        }
        return existingFieldTypeNames;
    }

    private void insertUnderTestField(PsiClass psiClass, String fullyQualifiedTypeName) {
        PsiClassType subjectClassType = PsiType.getTypeByName(fullyQualifiedTypeName, project,
                GlobalSearchScope.projectScope(project));
        insertNewField(psiClass, subjectClassType, UNDER_TEST_FIELD_NAME, null);
    }

    private void insertMockedField(PsiClass psiClass, PsiType psiType) {
        String newFieldName = suggestFieldName(psiType);

        newFieldName = Character.toLowerCase(newFieldName.charAt(0)) +
                newFieldName.substring(1, newFieldName.length());

        insertNewField(psiClass, psiType, newFieldName, MOCK_ANNOTATION_SHORT_NAME);
    }

    private void insertNewField(PsiClass psiClass, PsiType newFieldType, String newFieldName,
            @Nullable String annotationClassName) {
        PsiField underTestField = javaPsiFacade.getElementFactory().createField(newFieldName, newFieldType);
        if (annotationClassName != null) {
            underTestField.getModifierList().addAnnotation(annotationClassName);
        }
        psiClass.add(underTestField);
    }

    @NotNull
    private String suggestFieldName(PsiType psiType) {
        SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.FIELD, null, null, psiType);
        String name = info.names[0];
        return "m" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String getUnderTestQualifiedClassName(PsiClass psiClass) {
        String testClassName = psiClass.getQualifiedName();
        if (testClassName.endsWith(TEST_CLASS_NAME_SUFFIX)) {
            return testClassName.substring(0, testClassName.length() - TEST_CLASS_NAME_SUFFIX.length());
        }
        return null;
    }
}
