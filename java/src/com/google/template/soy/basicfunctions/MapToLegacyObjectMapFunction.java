/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.basicfunctions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyMap;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyLibraryAssistedJsSrcFunction;
import com.google.template.soy.plugin.java.restricted.JavaPluginContext;
import com.google.template.soy.plugin.java.restricted.JavaValue;
import com.google.template.soy.plugin.java.restricted.JavaValueFactory;
import com.google.template.soy.plugin.java.restricted.SoyJavaSourceFunction;
import com.google.template.soy.pysrc.restricted.PyExpr;
import com.google.template.soy.pysrc.restricted.SoyPySrcFunction;
import com.google.template.soy.shared.restricted.Signature;
import com.google.template.soy.shared.restricted.SoyFunctionSignature;
import com.google.template.soy.shared.restricted.TypedSoyFunction;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Converts values of type {@code map} to values of type {@code legacy_object_map}.
 *
 * <p>(This is the inverse of {@link LegacyObjectMapToMapFunction}.)
 *
 * <p>The two map types are designed to be incompatible in the Soy type system; the long-term plan
 * is to migrate all {@code legacy_object_map}s to {@code map}s and delete {@code
 * legacy_object_map}. To allow template-level migrations of {@code legacy_object_map} parameters to
 * {@code map}, we need plugins to convert between the two maps, so that converting one template
 * doesn't require converting its transitive callees.
 */
@SoyFunctionSignature(
    name = "mapToLegacyObjectMap",
    // Note: The return type is overridden in ResolveTypeExpressionsPass
    value = @Signature(parameterTypes = "map<any, any>", returnType = "?"))
public final class MapToLegacyObjectMapFunction extends TypedSoyFunction
    implements SoyJavaSourceFunction, SoyPySrcFunction, SoyLibraryAssistedJsSrcFunction {

  @Override
  public ImmutableSet<String> getRequiredJsLibNames() {
    return ImmutableSet.of("soy.map");
  }

  // lazy singleton pattern, allows other backends to avoid the work.
  private static final class Methods {
    static final Method MAP_TO_LEGACY_OBJECT_MAP =
        JavaValueFactory.createMethod(
            BasicFunctionsRuntime.class, "mapToLegacyObjectMap", SoyMap.class);
  }

  @Override
  public JavaValue applyForJavaSource(
      JavaValueFactory factory, List<JavaValue> args, JavaPluginContext context) {
    return factory.callStaticMethod(Methods.MAP_TO_LEGACY_OBJECT_MAP, args.get(0));
  }

  @Override
  public JsExpr computeForJsSrc(List<JsExpr> args) {
    return new JsExpr(
        "soy.map.$$mapToLegacyObjectMap(" + args.get(0).getText() + ")", Integer.MAX_VALUE);
  }

  @Override
  public PyExpr computeForPySrc(List<PyExpr> args) {
    String map = Iterables.getOnlyElement(args).getText();
    return new PyExpr(
        String.format("runtime.map_to_legacy_object_map(%s)", map), Integer.MAX_VALUE);
  }
}
