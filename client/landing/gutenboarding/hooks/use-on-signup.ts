/**
 * External dependencies
 */
import * as React from 'react';
import { useDispatch, useSelect } from '@wordpress/data';
import { useI18n } from '@automattic/react-i18n';

/**
 * Internal dependencies
 */
import { STORE_KEY as ONBOARD_STORE } from '../stores/onboard';
import { USER_STORE } from '../stores/user';
import { SITE_STORE } from '../stores/site';
import { useShouldSiteBePublic } from './use-selected-plan';

/**
 * After signup a site is automatically created using the username and bearerToken
 **/

export default function useOnSignup() {
	const { i18nLocale } = useI18n();
	const { createSite } = useDispatch( ONBOARD_STORE );

	const newUser = useSelect( ( select ) => select( USER_STORE ).getNewUser() );
	const newSite = useSelect( ( select ) => select( SITE_STORE ).getNewSite() );
	const shouldSiteBePublic = useShouldSiteBePublic();

	const handleCreateSite = React.useCallback(
		( username: string, bearerToken?: string, isPublicSite?: boolean ) => {
			createSite( username, i18nLocale, bearerToken, isPublicSite );
		},
		[ createSite ]
	);

	React.useEffect( () => {
		if ( newUser && newUser.bearerToken && newUser.username && ! newSite ) {
			handleCreateSite( newUser.username, newUser.bearerToken, shouldSiteBePublic );
		}
	}, [ newSite, newUser, i18nLocale, handleCreateSite, shouldSiteBePublic ] );
}
