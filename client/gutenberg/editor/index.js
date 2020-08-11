/**
 * External dependencies
 */
import page from 'page';

/**
 * Internal dependencies
 */
import { siteSelection, sites } from 'my-sites/controller';
import { authenticate, post, redirect, siteEditor, gutenbergWithoutIframe } from './controller';
import config from 'config';
import { makeLayout, render as clientRender } from 'controller';
import { performanceTrackerStart } from 'lib/performance-tracking/performance-tracker-start';

export default function () {
	page( '/block-editor', '/block-editor/post' );

	page(
		'/site-editor/:site?',
		siteSelection,
		redirect,
		authenticate,
		siteEditor,
		makeLayout,
		clientRender
	);

	page( '/block-editor/post', siteSelection, sites, makeLayout, clientRender );
	page(
		'/block-editor/post/:site/:post?',
		siteSelection,
		redirect,
		authenticate,
		post,
		makeLayout,
		clientRender
	);
	page( '/block-editor/post/:site?', siteSelection, redirect, makeLayout, clientRender );

	page(
		'/block-editor/without-iframe/post/:site/:post?',
		siteSelection,
		redirect,
		authenticate,
		performanceTrackerStart( 'without-iframe' ),
		gutenbergWithoutIframe,
		makeLayout,
		clientRender
	);

	page( '/block-editor/page', siteSelection, sites, makeLayout, clientRender );
	page(
		'/block-editor/page/:site/:post?',
		siteSelection,
		redirect,
		authenticate,
		post,
		makeLayout,
		clientRender
	);

	page(
		'/block-editor/without-iframe/page/:site/:post?',
		siteSelection,
		redirect,
		authenticate,
		gutenbergWithoutIframe,
		makeLayout,
		clientRender
	);

	page( '/block-editor/page/:site?', siteSelection, redirect, makeLayout, clientRender );

	if ( config.isEnabled( 'manage/custom-post-types' ) ) {
		page( '/block-editor/edit/:customPostType', siteSelection, sites, makeLayout, clientRender );
		page(
			'/block-editor/edit/:customPostType/:site/:post?',
			siteSelection,
			redirect,
			authenticate,
			post,
			makeLayout,
			clientRender
		);
		page(
			'/block-editor/edit/:customPostType/:site?',
			siteSelection,
			redirect,
			makeLayout,
			clientRender
		);
	}

	page( '/block-editor/*/*', '/block-editor/post' );
	page( '/block-editor/:site', ( context ) =>
		page.redirect( `/block-editor/post/${ context.params.site }` )
	);
}
