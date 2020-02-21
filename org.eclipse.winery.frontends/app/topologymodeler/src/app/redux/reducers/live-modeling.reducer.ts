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
import { LiveModelingStates, ServiceTemplateInstanceStates } from '../../models/enums';
import { Action } from 'redux';
import {
    LiveModelingActions, SetBuildPlanInputParametersAction, SetCurrentBuildPlanInstance, SetContainerUrlAction, SetCurrentCsarAction,
    SetCurrentCsarIdAction,
    SetCurrentServiceTemplateInstanceIdAction, SetCurrentServiceTemplateInstanceStateAction, SetDeploymentChangesAction, SetSettingsAction, SetStateAction
} from '../actions/live-modeling.actions';
import { InputParameter } from '../../models/container/input-parameter.model';
import { Csar } from '../../models/container/csar.model';
import { PlanInstance } from '../../models/container/plan-instance.model';

export interface LiveModelingState {
    state: LiveModelingStates;
    containerUrl: string;
    currentCsarId: string;
    currentCsar: Csar;
    currentServiceTemplateInstanceId: string;
    currentServiceTemplateInstanceState: ServiceTemplateInstanceStates;
    buildPlanInputParameters: InputParameter[];
    settings: any;
    deploymentChanges: boolean;
    currentBuildPlanInstance: PlanInstance;
}

export const INITIAL_LIVE_MODELING_STATE: LiveModelingState = {
    state: LiveModelingStates.DISABLED,
    containerUrl: null,
    currentCsarId: null,
    currentCsar: null,
    currentServiceTemplateInstanceId: null,
    currentServiceTemplateInstanceState: ServiceTemplateInstanceStates.INITIAL,
    buildPlanInputParameters: <InputParameter[]>[],
    settings: { timeout: 60000, interval: 1000 },
    deploymentChanges: false,
    currentBuildPlanInstance: null,
};

export const LiveModelingReducer =
    function (lastState: LiveModelingState = INITIAL_LIVE_MODELING_STATE, action: Action): LiveModelingState {
        switch (action.type) {
            case LiveModelingActions.SET_STATE:
                /**
                 const state = (<SetStateAction>action).state;
                 let nextState;

                 switch (lastState.state) {
                    case LiveModelingStates.DISABLED: {
                        if (state === LiveModelingStates.INIT || state === LiveModelingStates.UPDATE) {
                            nextState = state;
                        }
                        break;
                    }
                    case LiveModelingStates.INIT: {
                        if (state === LiveModelingStates.DEPLOY || state === LiveModelingStates.DISABLED) {
                            nextState = state;
                        }
                        break;
                    }
                    case LiveModelingStates.DEPLOY: {
                        if (state === LiveModelingStates.UPDATE) {
                            nextState = state;
                        }
                        break;
                    }
                    case LiveModelingStates.ENABLED: {
                        if (state === LiveModelingStates.REDEPLOY ||
                            state === LiveModelingStates.UPDATE ||
                            state === LiveModelingStates.TERMINATE ||
                            state === LiveModelingStates.DISABLED ||
                            state === LiveModelingStates.INIT
                        ) {
                            nextState = state;
                        }
                        break;
                    }
                    case LiveModelingStates.REDEPLOY: {
                        if (state === LiveModelingStates.UPDATE) {
                            nextState = state;
                        }
                        break;
                    }
                    case LiveModelingStates.UPDATE: {
                        if (state === LiveModelingStates.ENABLED) {
                            nextState = state;
                        }
                        break;
                    }
                    case LiveModelingStates.TERMINATE: {
                        if (state === LiveModelingStates.DISABLED) {
                            nextState = state;
                        }
                        break;
                    }
                    case LiveModelingStates.ERROR: {
                        nextState = state;
                    }
                }

                 if (!nextState) {
                    nextState = LiveModelingStates.ERROR;
                }
                 **/

                const state = (<SetStateAction>action).state;
                return <LiveModelingState>{
                    ...lastState,
                    state: state
                };
            case LiveModelingActions.SET_CONTAINER_URL: {
                const containerUrl = (<SetContainerUrlAction>action).containerUrl;

                return <LiveModelingState>{
                    ...lastState,
                    containerUrl: containerUrl
                };
            }
            case LiveModelingActions.SET_CURRENT_CSAR_ID: {
                const csarId = (<SetCurrentCsarIdAction>action).csarId;

                return <LiveModelingState>{
                    ...lastState,
                    currentCsarId: csarId
                };
            }
            case LiveModelingActions.SET_CURRENT_CSAR: {
                const csar = (<SetCurrentCsarAction>action).csar;

                return <LiveModelingState>{
                    ...lastState,
                    currentCsar: csar
                };
            }
            case LiveModelingActions.SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_ID: {
                const serviceTemplateInstanceId = (<SetCurrentServiceTemplateInstanceIdAction>action).serviceTemplateInstanceId;

                return <LiveModelingState>{
                    ...lastState,
                    currentServiceTemplateInstanceId: serviceTemplateInstanceId
                };
            }
            case LiveModelingActions.SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_STATE: {
                const serviceTemplateInstanceState = (<SetCurrentServiceTemplateInstanceStateAction>action).serviceTemplateInstanceState;

                return <LiveModelingState>{
                    ...lastState,
                    currentServiceTemplateInstanceState: serviceTemplateInstanceState
                };
            }
            case LiveModelingActions.SET_BUILD_PLAN_INPUT_PARAMETERS: {
                const inputParameters = (<SetBuildPlanInputParametersAction>action).inputParameters;

                return <LiveModelingState>{
                    ...lastState,
                    buildPlanInputParameters: inputParameters
                };
            }
            case LiveModelingActions.SET_SETTINGS: {
                const settings = (<SetSettingsAction>action).settings;

                return <LiveModelingState>{
                    ...lastState,
                    settings: settings
                };
            }
            case LiveModelingActions.SET_DEPLOYMENT_CHANGES: {
                const deploymentChanges = (<SetDeploymentChangesAction>action).deploymentChanges;

                return <LiveModelingState>{
                    ...lastState,
                    deploymentChanges: deploymentChanges
                };
            }
            case LiveModelingActions.SET_CURRENT_BUILD_PLAN_INSTANCE: {
                const buildPlanInstance = (<SetCurrentBuildPlanInstance>action).buildPlanInstance;

                return <LiveModelingState>{
                    ...lastState,
                    currentBuildPlanInstance: buildPlanInstance
                };
            }
            default: {
                return <LiveModelingState>lastState;
            }
        }
    };
