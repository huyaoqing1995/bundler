package in.workarounds.bundler.compiler.util.names;

import com.squareup.javapoet.ClassName;

import in.workarounds.bundler.compiler.model.ReqBundlerModel;

/**
 * Created by madki on 14/11/15.
 */
public class ClassProvider {

    private ClassProvider() {
    }

    // android classes
    public static final ClassName context = ClassName.get("android.content", "Context");
    public static final ClassName bundle = ClassName.get("android.os", "Bundle");
    public static final ClassName intent = ClassName.get("android.content", "Intent");

    public static final ClassName bundler = ClassName.get("in.workarounds.bundler", "Bundler");

    public static ClassName helper(ReqBundlerModel model) {
        return ClassName.bestGuess(model.getPackageName() + "." + model.getSimpleName() + "Bundler");
    }

    public static ClassName parser(ReqBundlerModel model) {
        return innerClass(helper(model), "Parser");
    }

    public static ClassName builder(ReqBundlerModel model) {
        return innerClass(helper(model), "Builder");
    }

    public static ClassName keys(ReqBundlerModel model) {
        return innerClass(helper(model), "Keys");
    }

    private static ClassName innerClass(ClassName superClass, String innerClass) {
        return ClassName.bestGuess(superClass.toString() + "." + innerClass);
    }
}
