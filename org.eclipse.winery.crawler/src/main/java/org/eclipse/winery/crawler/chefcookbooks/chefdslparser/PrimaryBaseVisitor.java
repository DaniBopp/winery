/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.crawler.chefcookbooks.chefdslparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.eclipse.winery.crawler.chefcookbooks.chefcookbook.CookbookParseResult;
import org.eclipse.winery.crawler.chefcookbooks.helper.ChefDslHelper;
import org.eclipse.winery.crawler.chefcookbooks.helper.RubyFunctionHelper;

import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrimaryBaseVisitor extends CollectionVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaryBaseVisitor.class.getName());

    private CookbookParseResult extractedCookbookConfigs;

    public PrimaryBaseVisitor(CookbookParseResult cookbookConfigurations) {
        super(cookbookConfigurations);
        this.extractedCookbookConfigs = cookbookConfigurations;
    }

    @Override
    public List<String> visitArgPrimary(ChefDSLParser.ArgPrimaryContext ctx) {
        List<String> attributeValue = null;
        PrimaryBaseVisitor argPrimaryVisitor = new PrimaryBaseVisitor(extractedCookbookConfigs);
        attributeValue = ctx.primary().accept(argPrimaryVisitor);

        if (attributeValue == null) {
            attributeValue = new ArrayList<>();
        }

        return attributeValue;
    }

    @Override
    public List<String> visitVarname(ChefDSLParser.VarnameContext ctx) {
        List<String> attributeValue = new ArrayList<>();
        String varName = ctx.getText();
        String varValue;
        try {
            varValue = CookbookVisitor.variables.get(varName);
        } catch (Exception e) {
            varValue = varName;
            e.printStackTrace();
        }
        attributeValue.add(varValue);

        return attributeValue;
    }

    @Override
    public List<String> visitString(ChefDSLParser.StringContext ctx) {
        List<String> attributeValue = new ArrayList<>();
        String literal = ctx.getChild(0).getText();
        int stringLength = literal.length();
        literal = literal.substring(1, stringLength - 1);
        if (ChefDslHelper.hasChefAttributeInString(literal)) {
            literal = ChefDslHelper.resolveRubyStringWithCode(extractedCookbookConfigs, literal);
        }
        attributeValue.add(literal);
        return attributeValue;
    }

    @Override
    public List<String> visitLitSymbol(ChefDSLParser.LitSymbolContext ctx) {
        List<String> attributeValue = new ArrayList<>();
        String literal = ctx.getChild(0).getText();
        attributeValue.add(literal);
        return attributeValue;
    }

    @Override
    public List<String> visitCaseStatement(ChefDSLParser.CaseStatementContext ctx) {
        List<String> attributeValue = new ArrayList<>();

        WhenArgsVisitor whenArgsVisitor;

        CaseConditionVisitor caseConditionVisitor;

        List<String> caseConditionList;
        String caseCondition;

        List<String> whenArgs = new ArrayList<>();

        boolean elseActive = false;

        caseConditionVisitor = new CaseConditionVisitor(extractedCookbookConfigs);
        caseConditionList = ctx.inner_comptstmt(0).accept(caseConditionVisitor);

        if (caseConditionList != null && caseConditionList.size() == 1) {

            caseCondition = (String) caseConditionList.get(0);
        } else {
            LOGGER.info("This should not happen! Case condition has more than one argument.");
            return attributeValue;
        }

        whenArgsVisitor = new WhenArgsVisitor(extractedCookbookConfigs);

        for (int iterChild = 2; iterChild < ctx.getChildCount(); iterChild++) { //counter starts at third index because first two childs can be ignored
            ParseTree child = ctx.getChild(iterChild);

            if (child instanceof ChefDSLParser.When_argsContext) {
                whenArgs = child.accept(whenArgsVisitor);
            } else if ("else".equals(child.getText())) {
                whenArgs.clear();
                elseActive = true;
            } else if (child instanceof ChefDSLParser.Inner_comptstmtContext) {
                PrimaryBaseVisitor primaryBaseVisitor = new PrimaryBaseVisitor(extractedCookbookConfigs);

                if (whenArgs.contains(caseCondition) || elseActive) {
                    attributeValue = child.accept(primaryBaseVisitor);

                    if (iterChild < ctx.getChildCount() - 1 && !(ctx.getChild(iterChild + 1) instanceof ChefDSLParser.Inner_comptstmtContext)) {
                        break;
                    }
                }
            }
        }

        return attributeValue;
    }

    @Override
    public List<String> visitPrimInt(ChefDSLParser.PrimIntContext ctx) {
        List<String> attributeValue = new ArrayList<>();
        String value = ctx.getText();
        attributeValue.add(value);
        return attributeValue;
    }

    @Override
    public List<String> visitPrimFloat(ChefDSLParser.PrimFloatContext ctx) {
        List<String> attributeValue = new ArrayList<>();
        String value = ctx.getText();
        attributeValue.add(value);
        return attributeValue;
    }

    @Override
    public List<String> visitPrimBoolean(ChefDSLParser.PrimBooleanContext ctx) {
        List<String> attributeValue = new ArrayList<>();
        String value = ctx.getText();
        attributeValue.add(value);
        return attributeValue;
    }

    @Override
    public List<String> visitPrim11(ChefDSLParser.Prim11Context ctx) {
        if (ctx.getText().startsWith("node") && !extractedCookbookConfigs.getAllConfigsAsList().isEmpty()) {
            return extractedCookbookConfigs.getAllConfigsAsList().get(0).getAttribute(ctx.getText().substring(4));
        }
        // Do not use SingletonList here
        return new ArrayList<>(Arrays.asList(ctx.getText()));
    }

    @Override
    public List<String> visitPrimOhaiFunc(ChefDSLParser.PrimOhaiFuncContext ctx) {
        List<String> attributeValue = new ArrayList<>();
        String literal;
        int stringLength;
        HashSet<String> arguments = new HashSet<>();
        String ohaiFunction = ctx.ohaiArg().getText();

        if (ctx.literal() != null) {
            for (ChefDSLParser.LiteralContext literalContext : ctx.literal()) {
                literal = literalContext.getText();
                stringLength = literal.length();
                literal = literal.substring(1, stringLength - 1);
                arguments.add(literal);
            }
        }

        if (extractedCookbookConfigs.getNumOfCookbookConfigs() == 1) {
            switch (ohaiFunction) {
                case "platform_family?":
                    attributeValue.add(Boolean.toString(extractedCookbookConfigs.getAllConfigsAsList().get(0).hasPlatformFamily(arguments)));
                    break;

                case "platform?":
                    attributeValue.add(Boolean.toString(extractedCookbookConfigs.getAllConfigsAsList().get(0).hasPlatform(arguments)));
                    break;

                default:
                    LOGGER.info("Ohai Function \"" + ohaiFunction + "\" is not implemented.");
                    attributeValue.add(null);
                    break;
            }
        } else {
            LOGGER.error("Parse result has " + extractedCookbookConfigs.getNumOfCookbookConfigs() + " cookbook configurations" +
                "\n At this point it should have only one cookbook configuration.");
        }
        return attributeValue;
    }

    @Override
    public List<String> visitPrimCompstmtInBrackets(ChefDSLParser.PrimCompstmtInBracketsContext ctx) {
        List<String> attributeValue;
        PrimaryBaseVisitor primaryBaseVisitor = new PrimaryBaseVisitor(extractedCookbookConfigs);
        attributeValue = ctx.inner_comptstmt().accept(primaryBaseVisitor);
        return attributeValue;
    }

    @Override
    public List<String> visitPrimFuncCall(ChefDSLParser.PrimFuncCallContext ctx) {
        List<String> convertedValueList = new ArrayList<>();
        PrimaryBaseVisitor booleanExprVisitor = new PrimaryBaseVisitor(extractedCookbookConfigs);
        List<String> primaryValue = ctx.primary().accept(booleanExprVisitor);

        if (primaryValue != null && ctx.function().getChildCount() == 1) {
            String functionName = ctx.function().getText();
            if ("to_i".equals(functionName)) {
                primaryValue.stream().filter(Objects::nonNull)
                    .forEach(s -> {
                        Integer convertedValue = RubyFunctionHelper.stringToInt(s);
                        if (convertedValue != null) {
                            convertedValueList.add(convertedValue.toString());
                        }
                    });
            }
        }

        return convertedValueList;
    }

    /**
     * This visit method should only be called in the evaluation of the ternary operator.
     */
    @Override
    public List<String> visitArgAssign(ChefDSLParser.ArgAssignContext ctx) {
        List<String> exprResult;
        PrimaryBaseVisitor primaryBaseVisitor = new PrimaryBaseVisitor(extractedCookbookConfigs);
        exprResult = ctx.arg().accept(primaryBaseVisitor);
        return exprResult;
    }

    public List<String> aggregateResult(List<String> aggregate, List<String> nextResult) {
        if (aggregate == null) {
            return nextResult;
        }
        if (nextResult == null) {
            return aggregate;
        }
        aggregate.addAll(nextResult);
        return aggregate;
    }
}
