/**
 * External dependencies
 */
import * as React from 'react';
import { useDispatch, useSelect } from '@wordpress/data';
import { useEntityProp } from '@wordpress/core-data';
import { __ } from '@wordpress/i18n';
import { TextControl, Tip } from '@wordpress/components';
import { Title, SubTitle, ActionButtons, NextButton } from '@automattic/onboarding';

/**
 * Internal dependencies
 */
import { LAUNCH_STORE } from '../../stores';
import { LaunchStep } from '../../../../common/data-stores/launch/data';
import LaunchStepContainer, { Props as LaunchStepProps } from '../../launch-step';
import './styles.scss';

const NameStep: React.FunctionComponent< LaunchStepProps > = ( { onNextStep } ) => {
	const domain = useSelect( ( select ) => select( LAUNCH_STORE ).getSelectedDomain() );
	const { setStepComplete, setStepIncomplete, setDomainSearch } = useDispatch( LAUNCH_STORE );
	const [ title, setTitle ] = useEntityProp( 'root', 'site', 'title' );
	const { saveEditedEntityRecord } = useDispatch( 'core' );

	const handleSave = () => {
		const newTitle = title.trim();

		setTitle( newTitle );
		saveEditedEntityRecord( 'root', 'site' );

		// update domainSearch only if there is no custom Domain selected
		! domain && setDomainSearch( newTitle );

		if ( newTitle ) {
			setStepComplete( LaunchStep.Name );
		} else {
			setStepIncomplete( LaunchStep.Name );
		}
	};

	const handleNext = () => {
		handleSave();
		onNextStep?.();
	};

	return (
		<LaunchStepContainer className="nux-launch-name-step">
			<div className="nux-launch-step__header">
				<div>
					<Title>{ __( 'Name your site', 'full-site-editing' ) }</Title>
					<SubTitle>{ __( 'Pick a name for your site.', 'full-site-editing' ) }</SubTitle>
				</div>
				<ActionButtons>
					<NextButton onClick={ handleNext } disabled={ ! title?.trim() } />
				</ActionButtons>
			</div>
			<div className="nux-launch-step__body">
				<form onSubmit={ handleNext }>
					<TextControl
						id="nux-launch-step__input"
						className="nux-launch-step__input"
						onChange={ setTitle }
						onBlur={ handleSave }
						value={ title }
						spellCheck={ false }
						autoComplete="off"
						placeholder={ __( 'Enter site name', 'full-site-editing' ) }
						autoCorrect="off"
						data-hj-whitelist
					/>
					<p className="nux-launch-step__input-hint">
						<Tip size={ 18 } />
						{ /* translators: The "it" here refers to the site title. */ }
						<span>{ __( "Don't worry, you can change it later.", 'full-site-editing' ) }</span>
					</p>
				</form>
			</div>
		</LaunchStepContainer>
	);
};

export default NameStep;
