import { useFormikContext } from 'formik'
import { useEffect, useState } from 'react'
import { detectTabWithError } from '../utils/helpers'

const useTabbedForm = (fieldsMap) => {
  const [activeTab, setActiveTab] = useState(0)
  const { isSubmitting, validateForm } = useFormikContext()

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue)
  }

  useEffect(() => {
    validateForm().then(errors => {
      const tabWithError = detectTabWithError(fieldsMap, errors)
      if (isSubmitting && (tabWithError || tabWithError === 0)) {
        setActiveTab(tabWithError)
      }
    })
  }, [isSubmitting, fieldsMap, setActiveTab, validateForm])

  return [activeTab, handleTabChange]
}


export default useTabbedForm
