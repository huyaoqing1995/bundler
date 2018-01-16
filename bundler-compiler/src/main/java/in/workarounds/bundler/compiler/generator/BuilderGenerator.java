package in.workarounds.bundler.compiler.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import in.workarounds.bundler.compiler.model.ArgModel;
import in.workarounds.bundler.compiler.model.ReqBundlerModel;
import in.workarounds.bundler.compiler.util.names.ClassProvider;
import in.workarounds.bundler.compiler.util.names.MethodName;
import in.workarounds.bundler.compiler.util.names.VarName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Created by madki on 17/11/15.
 */
public class BuilderGenerator {
    private ReqBundlerModel model;

    public BuilderGenerator(ReqBundlerModel model) {
        this.model = model;
    }

    public TypeSpec createClass() {
        MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();

        TypeSpec.Builder builder = TypeSpec.classBuilder(ClassProvider.builder(model).simpleName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethod(constructor);

        for (ArgModel arg : model.getArgs()) {
            builder.addField(arg.getAsField(Modifier.PRIVATE));
            builder.addMethod(setterMethod(arg));
        }

        if (isActivity()) {
            builder.addField(TypeName.INT, "flags", Modifier.PRIVATE);
            builder.addMethod(flagsMethod());
            generateField(builder, TypeName.INT, "enterAnimRes");
            generateField(builder, TypeName.INT, "exitAnimRes");
            generateField(builder, ClassProvider.bundle, "options");
        }

        builder.addMethod(bundleMethod());
        builder.addMethods(additionalMethods());

        return builder.build();
    }

    private MethodSpec flagsMethod() {
        return MethodSpec.methodBuilder("addFlag")
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassProvider.builder(model))
            .addParameter(TypeName.INT, "flag")
            .addStatement("this.flags |= flag")
            .addStatement("return this")
            .build();
    }

    private MethodSpec setterMethod(ArgModel arg) {
        return setter(arg.getAsParameter());
    }

    private MethodSpec setter(ParameterSpec type) {
        return MethodSpec.methodBuilder(type.name)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassProvider.builder(model))
            .addParameter(type)
            .addStatement("this.$1L = $1L", type.name)
            .addStatement("return this")
            .build();
    }

    private void generateField(TypeSpec.Builder builder, TypeName type, String name) {
        builder.addField(type, name, Modifier.PRIVATE);
        builder.addMethod(setter(ParameterSpec.builder(type, name).build()));
    }

    private List<MethodSpec> additionalMethods() {
        switch (model.getVariety()) {
            case ACTIVITY:
            case SERVICE:
                String methodName = isActivity() ? "startActivity" : "startService";
                return Arrays.asList(intentMethod(), startMethod(methodName));
            case FRAGMENT:
            case FRAGMENT_V4:
                return Arrays.asList(createMethod());
            case OTHER:
            default:
                return new ArrayList<>();
        }
    }

    private MethodSpec bundleMethod() {
        MethodSpec.Builder bundleBuilder = MethodSpec.methodBuilder(MethodName.bundle)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassProvider.bundle)
            .addStatement("$T $L = new $T()", ClassProvider.bundle, VarName.bundle, ClassProvider.bundle);

        for (ArgModel arg : model.getArgs()) {
            bundleBuilder.beginControlFlow("if($L != null)", arg.getLabel());
            ClassName serializer = arg.getSerializer();
            if (serializer != null) {
                bundleBuilder.addStatement("$L.put($T.$L, $L, $L)", VarName.from(serializer), ClassProvider.keys(model),
                    arg.getKeyConstant(), arg.getLabel(), VarName.bundle);
            } else {
                bundleBuilder.addStatement("$L.put$L($T.$L, $L)", VarName.bundle, arg.getBundleMethodSuffix(),
                    ClassProvider.keys(model), arg.getKeyConstant(), arg.getLabel());
            }
            bundleBuilder.endControlFlow();
        }

        bundleBuilder.addStatement("return $L", VarName.bundle);
        return bundleBuilder.build();
    }

    private MethodSpec intentMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(MethodName.intent)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassProvider.context, VarName.context)
            .returns(ClassProvider.intent)
            .addStatement("$T $L = new $T($L, $T.class)", ClassProvider.intent, VarName.intent, ClassProvider.intent,
                VarName.context, model.getClassName())
            .addStatement("$L.putExtras($L())", VarName.intent, MethodName.bundle);
        if (isActivity()) {
            builder.addStatement("$L.setFlags(flags)", VarName.intent);
        }
        return builder.addStatement("return $L", VarName.intent).build();
    }

    private MethodSpec startMethod(String methodName) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(MethodName.start)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassProvider.context, VarName.context);
        if (isActivity()) {
            return builder.beginControlFlow("if(options == null)")
                .addStatement("$L.$L($L($L))", VarName.context, methodName, MethodName.intent, VarName.context)
                .nextControlFlow("else")
                .addStatement("$L.$L($L($L), options)", VarName.context, methodName, MethodName.intent, VarName.context)
                .endControlFlow()
                .beginControlFlow("if($L instanceof $T)", VarName.context, ClassProvider.activity)
                .addStatement("(($T) $L).overridePendingTransition(enterAnimRes, exitAnimRes)", ClassProvider.activity,
                    VarName.context)
                .endControlFlow()
                .build();
        } else {
            return builder.addStatement("$L.$L($L($L))", VarName.context, methodName, MethodName.intent, VarName.context).build();
        }
    }

    private MethodSpec createMethod() {
        return MethodSpec.methodBuilder(MethodName.create)
            .addModifiers(Modifier.PUBLIC)
            .returns(model.getClassName())
            .addStatement("$T $L = new $T()", model.getClassName(), VarName.from(model), model.getClassName())
            .addStatement("$L.setArguments($L())", VarName.from(model), VarName.bundle)
            .addStatement("return $L", VarName.from(model))
            .build();
    }

    private boolean isActivity() {
        return model.getVariety() == ReqBundlerModel.VARIETY.ACTIVITY;
    }
}
