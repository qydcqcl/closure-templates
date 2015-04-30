/*
 * Copyright 2015 Google Inc.
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

package com.google.template.soy.jbcsrc;

import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.SoyError;
import com.google.template.soy.exprtree.AbstractExprNodeVisitor;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.ExprNode.ParentExprNode;
import com.google.template.soy.exprtree.VarDefn;
import com.google.template.soy.exprtree.VarRefNode;
import com.google.template.soy.soytree.AbstractSoyNodeVisitor;
import com.google.template.soy.soytree.AutoescapeMode;
import com.google.template.soy.soytree.ExprUnion;
import com.google.template.soy.soytree.SoyFileNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.SoyNode.ExprHolderNode;
import com.google.template.soy.soytree.SoyNode.ParentSoyNode;
import com.google.template.soy.soytree.TemplateNode;
import com.google.template.soy.soytree.defn.TemplateParam;
import com.google.template.soy.soytree.defn.TemplateParam.DeclLoc;

/**
 * A visitor that scans for features not supported by jbcsrc and reports errors for them.
 */
final class UnsupportedFeatureReporter {
  private final SoyNodeVisitor errorChecker;

  UnsupportedFeatureReporter(ErrorReporter errorReporter) {
    this.errorChecker = new SoyNodeVisitor(errorReporter);
  }

  void check(SoyNode node) {
    errorChecker.exec(node);
  }
  
  private static class SoyNodeVisitor extends AbstractSoyNodeVisitor<Void> {
    final ExprNodeVisitor exprVisitor;
    SoyNodeVisitor(ErrorReporter errorReporter) {
      super(errorReporter);
      this.exprVisitor = new ExprNodeVisitor(errorReporter);
    }

    @Override protected void visitTemplateNode(TemplateNode node) {
      super.visitTemplateNode(node);
      for (TemplateParam param : node.getAllParams()) {
        if (param.declLoc() == DeclLoc.SOY_DOC) {
          errorReporter.report(node.getSourceLocation(), 
              SoyError.of(
                  "jbcsrc doesn't support Soy doc style params, use {@param ...} instead"));
        }
      }
    }
    
    @Override protected void visitSoyFileNode(SoyFileNode node) {
      super.visitSoyFileNode(node);
      if (node.getDefaultAutoescapeMode() != AutoescapeMode.STRICT) {
        errorReporter.report(
            node.getSourceLocation(), 
            SoyError.of("jbcsrc only supports strict autoescape templates, found : ''{0}''"), 
            node.getDefaultAutoescapeMode());
      }
    }
    
    @Override protected void visitSoyNode(SoyNode node) {
      if (node instanceof ParentSoyNode<?>) {
        visitChildren((ParentSoyNode<?>) node);
      }
      if (node instanceof ExprHolderNode) {
        for (ExprUnion exprUnion : ((ExprHolderNode) node).getAllExprUnions()) {
          if (exprUnion.getExpr() == null) {
            errorReporter.report(node.getSourceLocation(), 
                SoyError.of("jbcsrc does not support soy v1 expressions: {0}"), 
                exprUnion.getExprText());
          } else {
            exprVisitor.exec(exprUnion.getExpr());
          }
        }
      }
    }
  }

  private static class ExprNodeVisitor extends AbstractExprNodeVisitor<Void> {
    ExprNodeVisitor(ErrorReporter errorReporter) {
      super(errorReporter);
    }

    @Override protected final void visitVarRefNode(VarRefNode node) {
      VarDefn defn = node.getDefnDecl();
      switch (defn.kind()) {
        case IJ_PARAM:
          errorReporter.report(node.getSourceLocation(), 
              SoyError.of(
                  "jbcsrc doesn't support $ij access, declare {@inject ..} params instead)"));
          break;
        case LOCAL_VAR:
        case PARAM:
          break;
        case UNDECLARED:
          errorReporter.report(node.getSourceLocation(),
              SoyError.of("jbcsrc does not support undeclared template params"));
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override protected void visitExprNode(ExprNode node) {
      if (node instanceof ParentExprNode) {
        visitChildren((ParentExprNode) node);
      }
    }
  }
}
