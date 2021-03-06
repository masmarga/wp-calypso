/**
 * External dependencies
 */
import * as React from 'react';
import { useHistory } from 'react-router-dom';
import { useSelect, useDispatch } from '@wordpress/data';
import { useViewportMatch } from '@wordpress/compose';
import { useI18n } from '@automattic/react-i18n';
import { SkipButton, NextButton } from '@automattic/onboarding';

/**
 * Internal dependencies
 */
import { STORE_KEY } from '../../stores/onboard';
import { Step, usePath } from '../../path';
import VerticalSelect from './vertical-select';
import SiteTitle from './site-title';
import { useTrackStep } from '../../hooks/use-track-step';
import { useShowVerticalInput } from '../../hooks/use-show-vertical-input';
import { recordVerticalSkip, recordSiteTitleSkip } from '../../lib/analytics';
import Arrow from './arrow';

/**
 * Style dependencies
 */
import './style.scss';

const AcquireIntent: React.FunctionComponent = () => {
	const { __ } = useI18n();
	const { getSelectedVertical, getSelectedSiteTitle, wasVerticalSkipped } = useSelect( ( select ) =>
		select( STORE_KEY )
	);

	const siteTitleRef = React.useRef< HTMLInputElement >();

	const { skipSiteVertical } = useDispatch( STORE_KEY );

	const history = useHistory();
	const makePath = usePath();
	const nextStepPath = makePath( Step.DesignSelection );

	const [ isSiteTitleActive, setIsSiteTitleActive ] = React.useState( false );

	const isMobile = useViewportMatch( 'small', '<' );

	useTrackStep( 'IntentGathering', () => ( {
		selected_vertical_slug: getSelectedVertical()?.slug,
		selected_vertical_label: getSelectedVertical()?.label,
		has_selected_site_title: !! getSelectedSiteTitle(),
	} ) );

	const hasSiteTitle = !! getSelectedSiteTitle();
	const showSiteTitleAndNext = !! ( getSelectedVertical() || hasSiteTitle || wasVerticalSkipped() );

	const handleSkip = () => {
		skipSiteVertical();
		recordVerticalSkip();
		setIsSiteTitleActive( true );
	};

	const onNext = () => {
		if ( isMobile ) {
			window.scrollTo( 0, 0 );
		}
		setIsSiteTitleActive( true );
		siteTitleRef.current?.focus();
	};

	const handleSiteTitleSubmit = () => {
		history.push( nextStepPath );
		! hasSiteTitle && recordSiteTitleSkip();
	};

	// declare UI elements here to avoid duplication when returning for mobile/desktop layouts
	const verticalSelect = <VerticalSelect onNext={ onNext } />;
	const siteTitleInput = showSiteTitleAndNext && (
		<SiteTitle inputRef={ siteTitleRef } onSubmit={ handleSiteTitleSubmit } />
	);
	const nextStepButton = hasSiteTitle ? (
		<NextButton onClick={ handleSiteTitleSubmit }>{ __( 'Choose a design' ) }</NextButton>
	) : (
		<SkipButton onClick={ handleSiteTitleSubmit }>{ __( 'I donʼt know' ) }</SkipButton>
	);

	const skipButton = (
		<SkipButton className="acquire-intent__skip-vertical" onClick={ handleSkip }>
			{ __( 'I donʼt know' ) }
		</SkipButton>
	);

	const siteVertical = getSelectedVertical();

	const showVerticalInput = useShowVerticalInput();

	return (
		<div className="gutenboarding-page acquire-intent">
			{ showVerticalInput ? (
				<>
					{ isMobile &&
						( isSiteTitleActive ? (
							<div>
								<Arrow
									className="acquire-intent__mobile-back-arrow"
									transform="rotate(180)"
									onClick={ () => setIsSiteTitleActive( false ) }
									role="button"
								/>

								{ siteTitleInput }
							</div>
						) : (
							verticalSelect
						) ) }
					{ ! isMobile && (
						<>
							{ ! wasVerticalSkipped() && verticalSelect }
							{ siteTitleInput }
						</>
					) }
					<div className="acquire-intent__footer">
						{ /* On mobile we render skipButton on vertical step when there is no vertical with more than 2 characters selected which is the
						case when we render the Next arrow button next to the input. On site title step we always render nextStepButton */ }
						{ isMobile &&
							( isSiteTitleActive
								? nextStepButton
								: ( ( ! siteVertical || siteVertical?.label?.length < 3 ) && skipButton ) || (
										<Arrow
											className="acquire-intent__mobile-next-arrow"
											onClick={ onNext }
											role="button"
										/>
								  ) ) }

						{ /* On desktop we always render nextStepButton when we render site title
						Otherwise we render skipButton  */ }
						{ ! isMobile && ( showSiteTitleAndNext ? nextStepButton : skipButton ) }
					</div>
				</>
			) : (
				<>
					<SiteTitle inputRef={ siteTitleRef } onSubmit={ handleSiteTitleSubmit } />
					<div className="acquire-intent__footer">{ nextStepButton }</div>
				</>
			) }
		</div>
	);
};

export default AcquireIntent;
