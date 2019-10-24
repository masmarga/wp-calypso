/**
 * Internal dependencies
 */
import { isHttps } from 'lib/url';
import getRawSite from 'state/selectors/get-raw-site';
import getSiteOption from './get-site-option';
import { getSitePreviewDisabled } from 'state/sites/preview/selectors';

/**
 * Returns true if the site can be previewed, false if the site cannot be
 * previewed, or null if preview ability cannot be determined. This indicates
 * whether it is safe to embed iframe previews for the site.
 *
 * @param  {Object}   state  Global state tree
 * @param  {Number}   siteId Site ID
 * @return {?Boolean}        Whether site is previewable
 */
export default function isSitePreviewable( state, siteId ) {
	if ( getSitePreviewDisabled( state, siteId ) ) {
		return false;
	}

	const site = getRawSite( state, siteId );
	if ( ! site ) {
		return null;
	}

	if ( site.is_vip ) {
		return false;
	}

	const unmappedUrl = getSiteOption( state, siteId, 'unmapped_url' );
	return !! unmappedUrl && isHttps( unmappedUrl );
}
