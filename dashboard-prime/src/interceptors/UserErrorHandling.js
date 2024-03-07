import axios from 'axios';
import { useRouter } from 'vue-router'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useErrorHandling = () => {

  const router = useRouter()
  const authState = useAuthState()
  const appConfig = useAppConfig()

  const errorResponseHandler = (error) => {

    console.log('handling error')
    console.log(error)
    if (axios.isCancel(error)) {
      console.log('isCancel')
      return Promise.resolve({ data: {} });
    }

    // check if the caller wants to handle all errors
    if (Object.prototype.hasOwnProperty.call(error.config, 'handleError') && error.config.handleError === false) {
      console.log('reject')
      return Promise.reject(error);
    }

    const errorCode = error.response ? error.response.status : undefined;
    console.log(`Error code is ${errorCode}`)
    // check if the caller wants to handle a specific error status code
    if (Object.prototype.hasOwnProperty.call(error.config, 'handleErrorCode')) {
      if (Array.isArray(error.config.handleErrorCode)) {
        if (error.config.handleErrorCode.find((el) => el === errorCode)) {
          return Promise.reject(error);
        }
      } else if (typeof error.config.handleErrorCode === 'string' && error.config.handleErrorCode.contains(',')) {
        const arr = error.config.handleErrorCode.split(',');
        if (arr.find((el) => el === errorCode)) {
          return Promise.reject(error);
        }
      } else if (error.config.handleErrorCode === errorCode) {
        return Promise.reject(error);
      }
    }

    const path = window.location.pathname;
    if (errorCode === 401) {
      authState.clearAuthData()
      if (path !== '/skills-login') {
        let loginRoute = path !== '/' ? { name: 'Login', query: { redirect: path } } : { name: 'Login' };
        if (appConfig.isPkiAuthenticated) {
          loginRoute = path !== '/' ? { name: 'LandingPage', query: { redirect: path } } : { name: 'LandingPage' };
        }
        router.push(loginRoute);
      }
    } else if (errorCode === 403) {
      let explanation;
      let ec;
      let projectId;
      if (error.response && error.response.data && error.response.data.explanation) {
        ({ explanation, errorCode: ec, projectId } = error.response.data);
      }
      if (explanation && ec === 'private_project') {
        router.push({ name: 'PrivateProjectAccessRequestPage', params: { explanation, projectId } });
      } else {
        router.push({ name: 'NotAuthorizedPage', params: { explanation } });
      }
    } else if (errorCode === 404) {
      let explanation;
      if (error.response && error.response.data && error.response.data.explanation) {
        ({ explanation } = error.response.data);
      }
      router.push({ name: 'NotFoundPage', params: { explanation } });
    } else if (errorCode === 503 && error?.response?.data?.errorCode === 'DbUpgradeInProgress') {
      router.push({ name: 'DbUpgradeInProgressPage' });
      return Promise.reject(error);
    } else {
      router.push({ name: 'ErrorPage' });
    }
    return Promise.resolve({ data: {} });
  }

  const registerErrorHandling = () => {
    axios.interceptors.response.use(
      (response) => response,
      errorResponseHandler,
    );
  }

  return {
    registerErrorHandling
  }
}