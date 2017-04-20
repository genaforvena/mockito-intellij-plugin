package org.mockito.plugin.util;

//
// Workaround to have this class in Android studio
//

import com.intellij.psi.*;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class AnnotationUtil {

    public static boolean isAnnotated(@NotNull PsiModifierListOwner listOwner, @NonNls @NotNull String annotationFQN, boolean checkHierarchy) {
        return isAnnotated(listOwner, annotationFQN, checkHierarchy, true, (Set)null);
    }


    private static boolean isAnnotated(@NotNull PsiModifierListOwner listOwner, @NonNls @NotNull String annotationFQN, boolean checkHierarchy, boolean skipExternal, @Nullable Set<PsiMember> processed) {
        if(!listOwner.isValid()) {
            return false;
        } else {
            PsiModifierList modifierList = listOwner.getModifierList();
            if(modifierList == null) {
                return false;
            } else {
                PsiAnnotation annotation = modifierList.findAnnotation(annotationFQN);
                if(annotation != null) {
                    return true;
                } else {

                    if(checkHierarchy) {
                        int var10;
                        int var11;
                        if(listOwner instanceof PsiMethod) {
                            PsiMethod method = (PsiMethod)listOwner;
                            if(processed == null) {
                                processed = new THashSet();
                            }

                            if(!((Set)processed).add(method)) {
                                return false;
                            }

                            PsiMethod[] superMethods = method.findSuperMethods();
                            PsiMethod[] var9 = superMethods;
                            var10 = superMethods.length;

                            for(var11 = 0; var11 < var10; ++var11) {
                                PsiMethod superMethod = var9[var11];
                                if(isAnnotated(superMethod, annotationFQN, true, skipExternal, (Set)processed)) {
                                    return true;
                                }
                            }
                        } else if(listOwner instanceof PsiClass) {
                            PsiClass clazz = (PsiClass)listOwner;
                            if(processed == null) {
                                processed = new THashSet();
                            }

                            if(!((Set)processed).add(clazz)) {
                                return false;
                            }

                            PsiClass[] superClasses = clazz.getSupers();
                            PsiClass[] var16 = superClasses;
                            var10 = superClasses.length;

                            for(var11 = 0; var11 < var10; ++var11) {
                                PsiClass superClass = var16[var11];
                                if(isAnnotated(superClass, annotationFQN, true, skipExternal, (Set)processed)) {
                                    return true;
                                }
                            }
                        }
                    }

                    return false;
                }
            }
        }
    }

}

